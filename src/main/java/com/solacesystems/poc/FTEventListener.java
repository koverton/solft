package com.solacesystems.poc;

/**
 * Interface for applications interested in joining a shared Solace FT Cluster
 * and receiving FT state change events reflecting the state of your application's
 * membership in the cluster.
 *
 * @see FTMgr
 */
public interface FTEventListener {
    /**
     * Invoked by the {@link FTMgr} when this listening application becomes the Active member of the cluster.
     */
    public void onActive();

    /**
     * Invoked by the {@link FTMgr} when this listening application changes to become a Backup member of the cluster.
     */
    public void onBackup();
}
