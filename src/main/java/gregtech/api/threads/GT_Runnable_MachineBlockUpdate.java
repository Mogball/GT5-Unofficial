package gregtech.api.threads;

import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.IMachineBlockUpdateable;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Cable;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Fluid;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Item;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class GT_Runnable_MachineBlockUpdate implements Runnable {
    //used by runner thread
    private final int x, y, z;
    private final World world;
    private final Set<ChunkPosition> visited = new HashSet<>(80);

    //Threading
    private static final ThreadFactory THREAD_FACTORY= r -> {
        Thread thread=new Thread(r);
        thread.setName("GT_MachineBlockUpdate");
        return thread;
    };
    private static ExecutorService EXECUTOR_SERVICE;

    //This class should never be initiated outside of this class!
    private GT_Runnable_MachineBlockUpdate(World aWorld, int aX, int aY, int aZ) {
        this.world = aWorld;
        this.x = aX;
        this.y = aY;
        this.z = aZ;
    }

    /**
     * If the thread is idle, sets new values and remove the idle flag, otherwise, queue the cooridinates.
     */
    public static void setMachineUpdateValues(World aWorld, int aX, int aY, int aZ) {
        EXECUTOR_SERVICE.submit(new GT_Runnable_MachineBlockUpdate(aWorld,aX,aY,aZ));
    }

    public static void initExecutorService() {
        EXECUTOR_SERVICE =  Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),THREAD_FACTORY);
    }

    public static void shutdownExecutorService() {
        EXECUTOR_SERVICE.shutdown();
        try {
            EXECUTOR_SERVICE.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            GT_Mod.GT_FML_LOGGER.error("Well this interruption got interrupted...", e);
        }
        //terminates executor permanently
        EXECUTOR_SERVICE.shutdownNow();
    }

    private boolean shouldRecurse(TileEntity aTileEntity, int aX, int aY, int aZ) {
        //no check on IGregTechTileEntity as it should call the underlying meta tile isMachineBlockUpdateRecursive
        //if (aTileEntity instanceof IGregTechTileEntity) {
        //    return ((IGregTechTileEntity) aTileEntity).isMachineBlockUpdateRecursive();
        //}
        return (aTileEntity instanceof IMachineBlockUpdateable &&
                ((IMachineBlockUpdateable) aTileEntity).isMachineBlockUpdateRecursive()) ||
                GregTech_API.isMachineBlock(world.getBlock(aX, aY, aZ), world.getBlockMetadata(aX, aY, aZ));
    }

    private void causeUpdate(TileEntity tileEntity){
        //no check for IGregTechTileEntity as it should call the underlying meta tile onMachineBlockUpdate
        if (tileEntity instanceof IMachineBlockUpdateable) {
            ((IMachineBlockUpdateable) tileEntity).onMachineBlockUpdate();
        }
    }

    private void stepToUpdateMachine(int aX, int aY, int aZ) {
        if (!visited.add(new ChunkPosition(aX, aY, aZ)))
            return;
        TileEntity tTileEntity = world.getTileEntity(aX, aY, aZ);

        causeUpdate(tTileEntity);

        if (visited.size() < 5 || shouldRecurse(tTileEntity, aX, aY, aZ)) {
            stepToUpdateMachine(aX + 1, aY, aZ);
            stepToUpdateMachine(aX - 1, aY, aZ);
            stepToUpdateMachine(aX, aY + 1, aZ);
            stepToUpdateMachine(aX, aY - 1, aZ);
            stepToUpdateMachine(aX, aY, aZ + 1);
            stepToUpdateMachine(aX, aY, aZ - 1);
        }
    }

    @Override
    public void run() {
        try {
            stepToUpdateMachine(x, y, z);
        } catch (Exception e) {
            GT_Mod.GT_FML_LOGGER.error("Well this update was broken... " + new Coordinates(x,y,z,world), e);
        }
    }

    public static class Coordinates {
        public final int mX;
        public final int mY;
        public final int mZ;
        public final World mWorld;

        public Coordinates(int mX, int mY, int mZ, World mWorld) {
            this.mX = mX;
            this.mY = mY;
            this.mZ = mZ;
            this.mWorld = mWorld;
        }

        @Override
        public String toString() {
            return "Coordinates{" +
                    "mX=" + mX +
                    ", mY=" + mY +
                    ", mZ=" + mZ +
                    ", mWorld=" + mWorld.getProviderName()+ " @dimId " + mWorld.provider.dimensionId +
                    '}';
        }
    }
}
