package com.yuqi.aeronauticsspace;

import com.yuqi.aeronauticsspace.item.ModItems;
import com.yuqi.aeronauticsspace.oxygen.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create Aeronautics: Space
 *
 * 让航空学的物理飞行器飞到"太空线"以上：失重 + 星空。
 */
@Mod(AeronauticsSpace.MODID)
public class AeronauticsSpace {

    public static final String MODID = "aeronautics_space";
    public static final Logger LOGGER = LoggerFactory.getLogger("AeronauticsSpace");

    public AeronauticsSpace(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModAttachments.register(modEventBus);
        LOGGER.info("[AeronauticsSpace] loaded - space line at Y={}, planets={}",
                SpaceAtmosphere.SPACE_LINE, com.yuqi.aeronauticsspace.planet.PlanetRegistry.all().size());
    }
}
