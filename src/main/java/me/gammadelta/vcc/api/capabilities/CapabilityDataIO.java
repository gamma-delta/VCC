package me.gammadelta.vcc.api.capabilities;

import me.gammadelta.vcc.api.IDataIOer;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public class CapabilityDataIO {
    @CapabilityInject(IDataIOer.class)
    public static Capability<IDataIOer> DATA_IO = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(IDataIOer.class, new Capability.IStorage<IDataIOer>() {
            // TODO: Do I actually need to write anything here?
            @Nullable
            @Override
            public INBT writeNBT(Capability<IDataIOer> capability, IDataIOer instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IDataIOer> capability, IDataIOer instance, Direction side, INBT nbt) { }
        }, IDataIOer.ReferenceImpl::new);
    }
}
