package de.mari_023.fabric.ae2wtlib.terminal;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.features.IWirelessTerminalHandler;
import appeng.api.features.Locatables;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.StorageChannels;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackList;
import appeng.api.util.DimensionalBlockPos;
import appeng.api.util.IConfigManager;
import appeng.blockentity.networking.WirelessBlockEntity;
import appeng.menu.interfaces.IInventorySlotAware;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

public abstract class WTGuiObject implements IGuiItemObject, IEnergySource, IActionHost, IInventorySlotAware {

    private final FixedViewCellInventory fixedViewCellInventory;
    private final ItemStack effectiveItem;
    private IGrid targetGrid;
    private final IWirelessTerminalHandler wth;
    private final PlayerEntity myPlayer;
    private IMEMonitor<IAEItemStack> itemStorage;
    private IWirelessAccessPoint myWap;
    private double sqRange = Double.MAX_VALUE;
    private double myRange = Double.MAX_VALUE;
    private IStorageService sg;
    private final int inventorySlot;
    private final IGridNode gridNode;

    public WTGuiObject(final IWirelessTerminalHandler wh, final ItemStack is, final PlayerEntity ep, int inventorySlot) {
        final OptionalLong unparsedKey = wh.getGridKey(is);
        effectiveItem = is;
        fixedViewCellInventory = new FixedViewCellInventory(is);
        myPlayer = ep;
        wth = wh;
        this.inventorySlot = inventorySlot;

        if(unparsedKey.isPresent()) {
            final IActionHost securityStation = Locatables.securityStations().get(ep.world, unparsedKey.getAsLong());
            if(securityStation != null) {
                gridNode = securityStation.getActionableNode();
                if(gridNode == null) return;
                targetGrid = gridNode.getGrid();
                sg = targetGrid.getStorageService();
                itemStorage = sg.getInventory(StorageChannels.items());
            } else gridNode = null;
        } else gridNode = null;
    }


    public abstract ScreenHandlerType<?> getType();

    public abstract ItemStack getIcon();

    public boolean notInRange() {
        boolean hasBoosterCard = ((IInfinityBoosterCardHolder) effectiveItem.getItem()).hasBoosterCard(effectiveItem);
        sqRange = myRange = Double.MAX_VALUE;

        if(targetGrid != null && itemStorage != null) {
            if(myWap != null) {
                if(myWap.getGrid() == targetGrid) return !testWap(myWap) && !hasBoosterCard;
                return !hasBoosterCard;
            } else isOutOfRange = true;

            for(final WirelessBlockEntity n : targetGrid.getMachines(WirelessBlockEntity.class)) {
                if(testWap(n)) myWap = n;
            }

            return myWap == null && !hasBoosterCard;
        }
        return !hasBoosterCard;
    }

    public double getRange() {
        return myRange;
    }

    private boolean isOutOfRange;

    public boolean isOutOfRange() {
        return isOutOfRange;
    }

    private boolean testWap(final IWirelessAccessPoint wap) {
        double rangeLimit = wap.getRange();
        rangeLimit *= rangeLimit;

        final DimensionalBlockPos dc = wap.getLocation();

        if(dc.getLevel() == myPlayer.world) {
            final double offX = dc.getPos().getX() - myPlayer.getX();
            final double offY = dc.getPos().getY() - myPlayer.getY();
            final double offZ = dc.getPos().getZ() - myPlayer.getZ();

            final double r = offX * offX + offY * offY + offZ * offZ;
            if(r < rangeLimit && sqRange > r && wap.isActive()) {
                sqRange = r;
                myRange = Math.sqrt(r);
                isOutOfRange = false;
                return true;
            }
        }
        isOutOfRange = true;
        return false;
    }

    @Override
    public IGridNode getActionableNode() {
        return gridNode;
    }

    @Override
    public int getInventorySlot() {
        return inventorySlot;
    }

    @Override
    public double extractAEPower(final double amt, final @NotNull Actionable mode, final @NotNull PowerMultiplier usePowerMultiplier) {
        if(wth != null && effectiveItem != null) {
            if(mode == Actionable.SIMULATE) return wth.hasPower(myPlayer, amt, effectiveItem) ? amt : 0;
            return wth.usePower(myPlayer, amt, effectiveItem) ? amt : 0;
        }
        return 0.0;
    }

    @Override
    public ItemStack getItemStack() {
        return effectiveItem;
    }

    public PlayerEntity getPlayer() {
        return myPlayer;
    }

    @NotNull
    public IConfigManager getConfigManager() {
        return wth.getConfigManager(getItemStack());
    }


    public <T extends IAEStack> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
        return sg.getInventory(channel);
    }

    public void addListener(final IMEMonitorHandlerReceiver<IAEItemStack> l, final Object verificationToken) {
        if(itemStorage != null) itemStorage.addListener(l, verificationToken);
    }

    public void removeListener(final IMEMonitorHandlerReceiver<IAEItemStack> l) {
        if(itemStorage != null) itemStorage.removeListener(l);
    }

    public IAEStackList<IAEItemStack> getAvailableItems(final IAEStackList<IAEItemStack> out) {
        if(itemStorage != null) return itemStorage.getAvailableItems();
        return out;
    }

    public IAEStackList<IAEItemStack> getStorageList() {
        if(itemStorage != null) return itemStorage.getStorageList();
        return null;
    }

    public AccessRestriction getAccess() {
        if(itemStorage != null) return itemStorage.getAccess();
        return AccessRestriction.NO_ACCESS;
    }

    public boolean isPrioritized(final IAEItemStack input) {
        if(itemStorage != null) return itemStorage.isPrioritized(input);
        return false;
    }

    public boolean canAccept(final IAEItemStack input) {
        if(itemStorage != null) return itemStorage.canAccept(input);
        return false;
    }

    public int getPriority() {
        if(itemStorage != null) return itemStorage.getPriority();
        return 0;
    }

    public boolean validForPass(final int i) {
        return itemStorage.validForPass(i);
    }

    public IAEItemStack injectItems(final IAEItemStack input, final Actionable type, final IActionSource src) {
        if(itemStorage != null) return itemStorage.injectItems(input, type, src);
        return input;
    }

    public IAEItemStack extractItems(final IAEItemStack request, final Actionable mode, final IActionSource src) {
        if(itemStorage != null) return itemStorage.extractItems(request, mode, src);
        return null;
    }

    public IStorageChannel getChannel() {
        return itemStorage != null ? itemStorage.getChannel() : StorageChannels.items();
    }

    public FixedViewCellInventory getViewCellStorage() {
        return fixedViewCellInventory;
    }
}