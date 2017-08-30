/**
 * <p>This Java library leverages Solace's guaranteed delivery capabilities to provide simple
 * Leader Election events to applications requiring fault tolerance, by
 * creating an instance of the {@link com.solacesystems.poc.FTMgr} and binding
 * a listener to it to receive FT role event updates.
 * </p>
 *
 * <p>To use it you must have a Solace Message Broker available for your application to
 * connect to for this library to work.
 * </p>
 *
 * <p>Here's a basic example of a program using the library to join a FT-cluster, then
 * print to the console any time its cluster membership role changes:</p>
 * <pre>
 * <code>
 *    new FTMgr(solaceConnectionProperties)
 *        .start(ftClusterName,
 *            new FTEventListener() {
 *                {@literal @}Override
 *                public void onActive() {
 *                    logger.info("STATE CHANGE TO ACTIVE");
 *                }
 *
 *                {@literal @}Override
 *                public void onBackup() {
 *                    logger.info("STATE CHANGE TO BACKUP");
 *                }
 *            });
 *    // ...
 * </code>
 * </pre>
 *
 * <p>First you will need a connected Solace session. You can either create a new session
 * just for <em>solft</em> by passing an instance of {@link java.util.Properties}, or more likely you'd pass a
 * shared Solace session, into your {@link com.solacesystems.poc.FTMgr} constructor.
 * You use the {@link com.solacesystems.poc.FTMgr} to join the
 * FT cluster and listen for events when cluster leadership state changes occur.</p>
 *
 * <p>To listen for these events you must implement a simple {@link com.solacesystems.poc.FTEventListener} interface
 * and pass the listener into the {@link com.solacesystems.poc.FTMgr#start(java.lang.String, com.solacesystems.poc.FTEventListener)}
 * method when starting the {@link com.solacesystems.poc.FTMgr}. All applications joining the same named cluster on
 * the same Solace broker will be part of the same FT-cluster, where a single application is elected leader until
 * it unbinds from the cluster for any reason. In that event, the cluster elects a new member and an
 * {@link com.solacesystems.poc.FTEventListener#onActive()} event is raised for that instance notifying it of this role change.</p>
 **/

package com.solacesystems.poc;
