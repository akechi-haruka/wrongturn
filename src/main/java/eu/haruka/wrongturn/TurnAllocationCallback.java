package eu.haruka.wrongturn;

/**
 * Interface for receiving allocation status updates.
 */
public interface TurnAllocationCallback {

    /**
     * Called when an allocation is allocated.
     */
    public void onAllocate(Allocation a);

    /**
     * Called when an allocation is freed (before connection and relay information is deleted).
     */
    public void onFree(Allocation a);

}
