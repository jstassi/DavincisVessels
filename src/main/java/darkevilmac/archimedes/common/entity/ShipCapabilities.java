package darkevilmac.archimedes.common.entity;

import darkevilmac.archimedes.ArchimedesShipMod;
import darkevilmac.archimedes.common.object.ArchimedesObjects;
import darkevilmac.archimedes.common.object.block.AnchorPointLocation;
import darkevilmac.archimedes.common.tileentity.TileEntityAnchorPoint;
import darkevilmac.archimedes.common.tileentity.TileEntityEngine;
import darkevilmac.archimedes.common.tileentity.TileEntityHelm;
import darkevilmac.movingworld.common.chunk.LocatedBlock;
import darkevilmac.movingworld.common.entity.EntityMovingWorld;
import darkevilmac.movingworld.common.entity.MovingWorldCapabilities;
import darkevilmac.movingworld.common.util.FloodFiller;
import darkevilmac.movingworld.common.util.LocatedBlockList;
import darkevilmac.movingworld.common.util.MaterialDensity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ShipCapabilities extends MovingWorldCapabilities {

    private final EntityShip ship;
    public float speedMultiplier, rotationMultiplier, liftMultiplier;
    public float brakeMult;
    private List<LocatedBlock> anchorPoints;
    private List<EntitySeat> seats;
    private List<TileEntityEngine> engines;
    private int balloonCount;
    private int floaters;
    private int blockCount;
    private int nonAirBlockCount;
    private float mass;

    private boolean canSubmerge = false;
    private boolean submerseFound = false;

    public ShipCapabilities(EntityMovingWorld movingWorld, boolean autoCalcMass) {
        super(movingWorld, autoCalcMass);
        ship = (EntityShip) movingWorld;
    }

    public float getSpeedMult() {
        return speedMultiplier + getEnginePower() * 0.5f;
    }

    public float getRotationMult() {
        return rotationMultiplier + getEnginePower() * 0.25f;
    }

    public float getLiftMult() {
        return liftMultiplier + getEnginePower() * 0.5f;
    }

    public float getEnginePower() {
        return ship.getDataWatcher().getWatchableObjectFloat(29);
    }

    public AnchorPointLocation findClosestValidAnchor(int range) {
        if (ship != null && ship.worldObj != null && !ship.worldObj.isRemote) {
            if (anchorPoints != null) {
                AnchorPointLocation apLoc = new AnchorPointLocation(null, null);
                List<AnchorPointLocation> validAnchorPoints = new ArrayList<AnchorPointLocation>();

                List<Integer> validAnchorPointsDistance = new ArrayList<Integer>();
                for (LocatedBlock anchorPointLB : anchorPoints) {
                    TileEntityAnchorPoint.AnchorPointInfo anchorPointInfo = ((TileEntityAnchorPoint) anchorPointLB.tileEntity).anchorPointInfo;
                    int infoPosX = anchorPointInfo.x;
                    int infoPosY = anchorPointInfo.y;
                    int infoPosZ = anchorPointInfo.z;

                    int differenceX = 0;
                    int differenceY = 0;
                    int differenceZ = 0;

                    boolean validXDistance = false;
                    boolean validYDistance = false;
                    boolean validZDistance = false;
                    boolean validDistance = false;

                    if (infoPosX > ship.posX) {
                        for (int i = 1; i < range; i++) {
                            if (ship.posX + i >= infoPosX) {
                                validXDistance = true;
                                differenceX = i;
                            }
                        }
                    } else {
                        for (int i = 1; i < range; i++) {
                            if (infoPosX + i >= ship.posX) {
                                validXDistance = true;
                                differenceX = i;
                            }
                        }
                    }

                    if (infoPosY > ship.posY) {
                        for (int i = 1; i < range; i++) {
                            if (ship.posY + i >= infoPosY) {
                                validYDistance = true;
                                differenceY = i;
                            }
                        }
                    } else {
                        for (int i = 1; i < range; i++) {
                            if (infoPosY + i >= ship.posY) {
                                validYDistance = true;
                                differenceY = i;
                            }
                        }
                    }

                    if (infoPosZ > ship.posZ) {
                        for (int i = 1; i < range; i++) {
                            if (ship.posZ + i >= infoPosZ) {
                                validZDistance = true;
                                differenceZ = i;
                            }
                        }
                    } else {
                        for (int i = 1; i < range; i++) {
                            if (infoPosZ + i >= ship.posZ) {
                                validZDistance = true;
                                differenceZ = i;
                            }
                        }
                    }

                    validDistance = validXDistance && validYDistance && validZDistance;

                    if (validDistance && ship.worldObj.getTileEntity(infoPosX, infoPosY, infoPosZ) != null
                            && ship.worldObj.getTileEntity(infoPosX, infoPosY, infoPosZ) instanceof TileEntityAnchorPoint) {
                        TileEntityAnchorPoint anchorPoint = (TileEntityAnchorPoint) ship.worldObj.getTileEntity(infoPosX, infoPosY, infoPosZ);
                        if (anchorPoint.anchorPointInfo != null && !anchorPoint.anchorPointInfo.forShip) {
                            AnchorPointLocation anchorPointLocation = new AnchorPointLocation(null, null);
                            World anchorPointWorld = anchorPoint.getWorldObj();
                            ChunkPosition anchorPointCoords = new ChunkPosition(anchorPoint.xCoord, anchorPoint.yCoord, anchorPoint.zCoord);

                            anchorPointLocation.worldAnchor = new LocatedBlock(anchorPointWorld.getBlock(anchorPointCoords.chunkPosX, anchorPointCoords.chunkPosY, anchorPointCoords.chunkPosZ),
                                    anchorPointWorld.getBlockMetadata(anchorPointCoords.chunkPosX, anchorPointCoords.chunkPosY, anchorPointCoords.chunkPosZ), anchorPointCoords);
                            anchorPointLocation.shipAnchor = anchorPointLB;

                            validAnchorPoints.add(anchorPointLocation);
                            validAnchorPointsDistance.add(differenceX + differenceY + differenceZ);
                        }
                    }
                }

                AnchorPointLocation shortestAnchorLocation = null;

                if (validAnchorPoints != null && !validAnchorPoints.isEmpty()) {
                    int shortestIndex = 0;
                    for (int index = 0; index < validAnchorPoints.size(); index++) {
                        if (validAnchorPointsDistance.get(index) < validAnchorPointsDistance.get(shortestIndex)) {
                            shortestIndex = index;
                        }
                    }
                    shortestAnchorLocation = validAnchorPoints.get(shortestIndex);
                }

                return shortestAnchorLocation;
            }
        }
        return null;
    }

    public void updateEngines() {
        float ePower = 0;
        if (engines != null) {
            for (TileEntityEngine te : engines) {
                te.updateRunning();
                if (te.isRunning()) {
                    ePower += te.enginePower;
                }
            }
        }
        if (!ship.worldObj.isRemote)
            ship.getDataWatcher().updateObject(29, ePower);
    }

    @Override
    public boolean canFly() {
        return (ArchimedesShipMod.instance.modConfig.enableAirShips && getBalloonCount() >= blockCount * ArchimedesShipMod.instance.modConfig.flyBalloonRatio)
                || ship.areSubmerged();
    }

    public boolean canSubmerge() {
        if (!submerseFound) {
            FloodFiller floodFiller = new FloodFiller();
            LocatedBlockList filledBlocks = floodFiller.floodFillMobileChunk(ship.getMobileChunk());
            int filledBlockCount = filledBlocks.size();
            canSubmerge = false;
            if (ArchimedesShipMod.instance.modConfig.enableSubmersibles)
                canSubmerge =
                        filledBlockCount < (nonAirBlockCount * ArchimedesShipMod.instance.modConfig.submersibleFillRatio);
            submerseFound = true;
        }
        return canSubmerge;
    }

    @Override
    public int getBlockCount() {
        return blockCount;
    }

    public int getBalloonCount() {
        return balloonCount;
    }

    public void setBalloonCount(int balloonCount) {
        this.balloonCount = balloonCount;
    }

    public int getFloaterCount() {
        return floaters;
    }

    @Override
    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public void addAttachments(EntitySeat entity) {
        if (seats == null) seats = new ArrayList<EntitySeat>();
        if (entity != null && entity instanceof EntitySeat) seats.add(entity);
    }

    public boolean canMove() {
        return ship.getDataWatcher().getWatchableObjectByte(28) == 1;
    }

    public List<EntitySeat> getAttachments() {
        return seats;
    }

    public List<TileEntityEngine> getEngines() {
        return engines;
    }

    public List<LocatedBlock> getAnchorPoints() {
        return anchorPoints;
    }

    @Override
    public void postBlockAdding() {
        if (ship.getMobileChunk() != null && ship.getMobileChunk().marker != null && ship.getMobileChunk().marker.tileEntity != null && ship.getMobileChunk().marker.tileEntity instanceof TileEntityHelm) {
            if (((TileEntityHelm) ship.getMobileChunk().marker.tileEntity).submerge && canSubmerge()) {
                ship.setSubmerge(true);
            }
        }
    }


    @Override
    public void onChunkBlockAdded(Block block, int metadata, int x, int y, int z) {
        mass += MaterialDensity.getDensity(block);

        blockCount++;
        nonAirBlockCount++;

        if (block == null) {
        } else if (block == ArchimedesObjects.blockFloater) {
            nonAirBlockCount--;
            return;
        }
        if (block instanceof BlockAir)
            nonAirBlockCount--;

        if (block == ArchimedesObjects.blockBalloon || ArchimedesShipMod.instance.modConfig.isBalloon(block)) {
            balloonCount++;
        } else if (block == ArchimedesObjects.blockFloater) {
            floaters++;
        } else if (block == ArchimedesShipMod.objects.blockAnchorPoint) {
            TileEntity te = ship.getMobileChunk().getTileEntity(x, y, z);
            if (te != null && te instanceof TileEntityAnchorPoint && ((TileEntityAnchorPoint) te).anchorPointInfo != null && ((TileEntityAnchorPoint) te).anchorPointInfo.forShip) {
                if (anchorPoints == null) {
                    anchorPoints = new ArrayList<LocatedBlock>();
                }
                anchorPoints.add(new LocatedBlock(block, metadata, te, new ChunkPosition(x, y, z), new ChunkPosition(0, 0, 0)));
            }
        } else if (block == ArchimedesObjects.blockEngine) {
            TileEntity te = ship.getMovingWorldChunk().getTileEntity(x, y, z);
            if (te instanceof TileEntityEngine) {
                if (engines == null) {
                    engines = new ArrayList<TileEntityEngine>(4);
                }
                engines.add((TileEntityEngine) te);
            }
        } else if ((block == ArchimedesObjects.blockSeat || ArchimedesShipMod.instance.modConfig.isSeat(block)) && !ship.worldObj.isRemote) {
            int x1 = ship.riderDestinationX, y1 = ship.riderDestinationY, z1 = ship.riderDestinationZ;
            if (ship.frontDirection == 0) {
                z1 -= 1;
            } else if (ship.frontDirection == 1) {
                x1 += 1;
            } else if (ship.frontDirection == 2) {
                z1 += 1;
            } else if (ship.frontDirection == 3) {
                x1 -= 1;
            }
            if (x != x1 || y != y1 || z != z1) {
                EntitySeat seat = new EntitySeat(ship.worldObj);
                seat.setParentShip(ship, x, y, z);
                addAttachments(seat);
            }
        }
    }

    public boolean hasSeat(EntitySeat seat) {
        if (seats != null && !seats.isEmpty()) {
            return seats.contains(seat);
        } else {
            return true;
        }
    }

    public EntitySeat getAvailableSeat() {
        for (EntitySeat seat : seats) {
            if (seat.riddenByEntity == null || (seat.riddenByEntity != null && (seat.riddenByEntity.ridingEntity == null
                    || (seat.riddenByEntity.ridingEntity != null && seat.riddenByEntity.ridingEntity != seat)))) {
                seat.mountEntity(null);
                return seat;
            }
        }
        return null;
    }

    @Override
    public boolean mountEntity(Entity entity) {
        if (seats == null) {
            return false;
        }

        for (EntitySeat seat : seats) {
            if (seat.interactFirst((EntityPlayer) entity)) {
                return true;
            }
        }
        return false;
    }

    public void spawnSeatEntities() {
        if (seats != null && !seats.isEmpty()) {
            for (EntitySeat seat : seats) {
                ship.worldObj.spawnEntityInWorld(seat);
            }
        }
    }

    @Override
    public void clearBlockCount() {
        speedMultiplier = rotationMultiplier = liftMultiplier = 1F;
        brakeMult = 0.9F;
        floaters = 0;
        blockCount = 0;
        mass = 0F;
        if (engines != null) {
            engines.clear();
            engines = null;
        }
    }

    @Override
    public void clear() {
        if (seats != null) {
            for (EntitySeat seat : seats) {
                seat.setDead();
            }
            seats = null;
        }
        if (engines != null) {
            engines.clear();
            engines = null;
        }
        submerseFound = false;
        canSubmerge = false;
        clearBlockCount();
    }

    @Override
    public float getSpeedLimit() {
        return ArchimedesShipMod.instance.modConfig.speedLimit;
    }

    @Override
    public float getBankingMultiplier() {
        return ArchimedesShipMod.instance.modConfig.bankingMultiplier;
    }


}
