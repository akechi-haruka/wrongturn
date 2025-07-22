package eu.haruka.wrongturn;

import eu.haruka.wrongturn.packets.Allocation;

public interface TurnAllocationCallback {

    public void onAllocate(Allocation a);

    public void onFree(Allocation a);

}
