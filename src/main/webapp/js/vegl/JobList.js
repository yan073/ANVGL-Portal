/**
 * Page for listing and monitoring running jobs
 */
Ext.application({
    name : 'portal',

    mixins: ['Ext.mixin.Responsive'],

    responsiveFormulas: {
        small: 'width < 1250',
        normal: 'width >= 1250'
    },

    init : function() {
        Ext.getBody().mask("Loading your jobs...");
    },

    //Here we build our GUI from existing components - this function should only be assembling the GUI
    //Any processing logic should be managed in dedicated classes - don't let this become a
    //monolithic 'do everything' function
    launch : function() {
        Ext.getBody().unmask();

        var showFatalError = function(message) {
            var viewport = Ext.ComponentQuery.query('viewport')[0];
            if (!viewport) {
                viewport = Ext.create('Ext.container.Viewport', {
                    layout: 'border',
                    items: []
                });
            }

            //Update everything after a short delay to ensure we catch everything
            new Ext.util.DelayedTask(function(){
                viewport.removeAll(true);
                viewport.add({
                    xtype : 'container',
                    region : 'center',
                    layout : {
                        type: 'vbox',
                        align: 'center',
                        pack: 'start'
                    },
                    style: {
                        'background-color': 'white'
                    },
                    items : [{
                        xtype : 'image',
                        width: 500,
                        height: 500,
                        src : '../img/alert.svg'
                    },{
                        xtype: 'panel',
                        width: '100%',
                        bodyStyle: {
                            'text-align': 'center',
                            'font-size': 24
                        },
                        html: message
                    }]
                });
            }).delay(500);
        };

        var onError = function(cmp, message) {
            portal.widgets.window.ErrorWindow.showText('Error', message, null);
        };

        //This is used for updating statuses of existing jobs that have been loaded
        var refreshJobStatus = function(jobStore, treePanel) {
            portal.util.Ajax.request({
                url: 'secure/jobsStatuses.do',
                params: {
                    forceStatusRefresh: true
                },
                callback: function(success, data, message, debugInfo) {
                    if (!success) {
                        portal.widgets.window.ErrorWindow.showText('Error', 'Unable to update your jobs due to an error. Please try refreshing the page.', debugInfo);
                        return;
                    }

                    var treeStore = treePanel.getStore();
                    for (var i = 0; i < data.length; i++) {
                        var tuple  = data[i];
                        var record = jobStore.getById(tuple.jobId);
                        if (record) {
                            record.set('status', tuple.status);
                        }

                        var node = treeStore.getNodeById(tuple.jobId.toString());
                        if (node) {
                            node.set('status', tuple.status);
                        }
                    }

                    //Update job details panel for jobs that can have updated files
                    var jobDetailsPanel = Ext.getCmp('job-details-panel');
                    if (jobDetailsPanel.job) {
                        switch(jobDetailsPanel.job.get('status')) {
                        case vegl.models.Job.STATUS_ACTIVE:
                        case vegl.models.Job.STATUS_PENDING:
                        case vegl.models.Job.STATUS_INQUEUE:
                        case vegl.models.Job.STATUS_PROVISIONING:
                            jobDetailsPanel.updateJobDetails();
                            break;
                        default:
                            jobDetailsPanel.updateSubmitTime();
                            break;
                        }
                    }
                }
            });
        };

        //This is used for refreshing the entire structure of a job tree (with an optional status refresh)
        var refreshRunning = false;
        var refreshJobNodes = function(jobStore, treePanel, forceStatusRefresh) {
            if (refreshRunning) {
                return;
            }
            refreshRunning = true;
            treePanel.getEl().mask('Loading...');
            portal.util.Ajax.request({
                url: 'secure/treeJobs.do',
                params: {
                    forceStatusRefresh: forceStatusRefresh ? true : false
                },
                callback: function(success, data, message, debugInfo) {
                    treePanel.getEl().unmask();
                    refreshRunning = false;
                    if (!success) {
                        portal.widgets.window.ErrorWindow.showText('Error', 'Unable to update your jobs due to an error. Please try refreshing the page.', debugInfo);
                        return;
                    }

                    jobStore.loadData(data.jobs);
                    treePanel.getStore().removeAll();
                    treePanel.getStore().fireEvent('clear', treePanel.getStore()); //This isn't fired in Ext 5.1.0 - we fire it manually
                    treePanel.getStore().setRoot(data.nodes);
                }
            });
        };

        var handleAddFolder = function(jobStore, treePanel) {
            Ext.MessageBox.show({
                title: 'Folder Name',
                message: 'Please enter a folder name',
                width: 300,
                buttons: Ext.Msg.OKCANCEL,
                icon: Ext.window.MessageBox.INFO,
                prompt: true,
                modal: true,
                fn: function(buttonId, text) {
                    if (buttonId === 'ok' && !Ext.isEmpty(text.trim())) {
                        portal.util.Ajax.request({
                            url: 'secure/createFolder.do',
                            params: {
                                seriesName: text.trim(),
                                seriesDescription: ''
                            },
                            success: function(data, message, debugInfo) {
                                refreshJobNodes(jobStore, treePanel);
                            },
                            failure: function(message, debugInfo) {
                                portal.widgets.window.ErrorWindow.showText('Error', 'Unable to create folder due to a server error. Please try refreshing the page.', debugInfo);
                            }
                        });
                    }
                }
            });
        };

        portal.util.Ajax.request({
            url: 'secure/treeJobs.do',
            callback: function(success, data, message) {
                if (!success) {
                    showFatalError("Unable to load your jobs due to an error. Please try refreshing the page.");
                    return;
                }

                var jobStore = Ext.create('Ext.data.Store', {
                    model: 'vegl.models.Job',
                    proxy: {
                        type: 'memory',
                        reader: {
                            type: 'json'
                        }
                    }
                });
                jobStore.loadData(data.jobs);

                var jobDetailsPanel = Ext.create('vegl.widgets.DetailsPanel', {
                    title: 'Description',
                    region: 'center',
                    id: 'job-details-panel',
                    split: true,
                    listeners : {
                        error : onError
                    }
                });

                var jobsTree = Ext.create('vegl.widgets.JobsTree', {
                    title: 'Your Jobs',
                    region: 'center',
                    stripeRows: true,
                    id : 'vgl-jobs-tree',
                    rootNode: data.nodes,
                    jobStore: jobStore,
                    selModel: {
                    	mode: 'MULTI'
                    },
                    listeners : {
                        selectjob : function(panel, job) {
                        	// Cleanup if no job or more than 1 job selected
                            if (job == null || this.getSelectionModel().getCount()>1) {
                                jobDetailsPanel.cleanupDetails();
                            } else {
                                jobDetailsPanel.showDetailsForJob(job);
                            }
                        },
                        refreshJobs: function(panel) {
                            refreshJobNodes(jobStore, panel);
                        },
                        refreshDetailsPanel: function(tree, job) {
                            if (job == null || job.get('status') === vegl.models.Job.STATUS_DELETED) {
                                jobDetailsPanel.cleanupDetails();
                            } else {
                                jobDetailsPanel.showDetailsForJob(job);
                            }
                        },
                        error : onError
                    },
                    viewConfig: {
                        plugins: {
                            ptype: 'treeviewdragdrop',
                            containerScroll: true
                        },
                        listeners: {
                            resize: function(treeView, width) {
                                var tree = treeView.grid;
                                if (width < 260) {
                                    tree.columns[1].setVisible(false);
                                    tree.columns[2].setVisible(false);
                                } else if (width < 420) {
                                    tree.columns[1].setVisible(false);
                                    tree.columns[2].setVisible(true);
                                } else {
                                    tree.columns[1].setVisible(true);
                                    tree.columns[2].setVisible(true);
                                }
                            },
                            drop: function(node, data, overModel, dropPosition) {
                                if (!overModel) {
                                    return;
                                }

                                var seriesId = overModel.get('seriesId');
                                var jobIds = [data.records.length];
                                for(i=0; i<data.records.length; i++)
                                	jobIds[i] = data.records[i].get('id');

                                portal.util.Ajax.request({
                                    url: 'secure/setJobFolder.do',
                                    params: {
                                        seriesId: seriesId,
                                        jobIds: jobIds
                                    },
                                    callback: function(success, data, message, debugInfo) {
                                        if (!success) {
                                            portal.widgets.window.ErrorWindow.showText('Error', 'There was an error reassigning job folders. Please refresh the page.', debugInfo);
                                        }
                                    }
                                });
                            },
                            afterrender: function(tree) {
                                var taskExecutionCount = 0; //We dont want this firing immediately
                                tree.refreshRunner = new Ext.util.TaskRunner();
                                tree.refreshRunner.start({
                                    run: function() {
                                        if (taskExecutionCount > 0) {
                                            refreshJobStatus(jobStore, jobsTree);
                                        }
                                        taskExecutionCount++;
                                    },
                                    interval: 60 * 1000 //60 Seconds
                                });
                            }
                        },

                    },
                    buttons: [{
                        text: 'Add Folder',
                        itemId : 'btnAddFolder',
                        tooltip : 'Add a new folder',
                        iconCls: 'folder-icon',
                        handler: function() {
                            handleAddFolder(jobStore, jobsTree);
                        }
                    },{
                        text: 'Refresh',
                        itemId : 'btnRefresh',
                        tooltip : 'Refresh the job statuses',
                        iconCls: 'refresh-icon',
                        handler: function() {
                            refreshJobStatus(jobStore, jobsTree, true);
                        }
                    }]
                });

                var me = this;

                var treeWidth = 500;
                if (window.screen.width < 900) {
                    treeWidth = 250;
                } else if (window.screen.width < 1000) {
                    treeWidth = 400;
                }
                Ext.create('Ext.container.Viewport', {
                    layout: 'border',
                    id : 'vgl-joblist-viewport',
                    style: {
                        'background-color': 'white'
                    },
                    items: [{
                        xtype: 'box',
                        region: 'north',
                        applyTo: 'body',
                        height: 60
                    },{
                        border: false,
                        region: 'west',
                        split: true,
                        margins: '2 2 2 0',
                        layout: 'border',
                        width: treeWidth,
                        bodyStyle: {
                            'background-color': 'white'
                        },
                        items: [jobsTree]
                    },jobDetailsPanel]
                });
            }
        });
    }

});

