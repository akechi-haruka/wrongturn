package eu.haruka.wrongturn;

/**
 * Interface for receiving allocation status updates.
 */
public interface TurnAllocationCallback {

    /**
     * Called when an allocation is allocated.
     */
    void onAllocate(Allocation a);

    /**
     * Called when an allocation is freed (before connection and relay information is deleted).
     */
    void onFree(Allocation a);

}
