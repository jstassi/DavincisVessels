package darkevilmac.archimedes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import darkevilmac.archimedes.common.ArchimedesConfig;
import darkevilmac.archimedes.common.CommonProxy;
import darkevilmac.archimedes.common.command.CommandASHelp;
import darkevilmac.archimedes.common.command.CommandDisassembleNear;
import darkevilmac.archimedes.common.command.CommandDisassembleShip;
import darkevilmac.archimedes.common.command.CommandShipInfo;
import darkevilmac.archimedes.common.entity.EntityParachute;
import darkevilmac.archimedes.common.entity.EntitySeat;
import darkevilmac.archimedes.common.entity.EntityShip;
import darkevilmac.archimedes.common.handler.ConnectionHandler;
import darkevilmac.archimedes.common.network.ArchimedesShipsMessageToMessageCodec;
import darkevilmac.archimedes.common.network.ArchimedesShipsPacketHandler;
import darkevilmac.archimedes.common.network.NetworkUtil;
import darkevilmac.archimedes.common.object.ArchimedesObjects;
import darkevilmac.movingworld.MovingWorld;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Mod(modid = ArchimedesShipMod.MOD_ID, name = ArchimedesShipMod.MOD_NAME, version = ArchimedesShipMod.MOD_VERSION, dependencies = "required-after:MovingWorld@", guiFactory = ArchimedesShipMod.MOD_GUIFACTORY)
public class ArchimedesShipMod {
    public static final String MOD_ID = "ArchimedesShipsPlus";
    public static final String MOD_VERSION = "@AS+VER@";
    public static final String MOD_NAME = "Archimedes' Ships Plus";
    public static final String MOD_GUIFACTORY = "darkevilmac.archimedes.client.gui.ArchimedesGUIFactory";

    public static CreativeTabs creativeTab = new CreativeTabs("archimedesTab") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(ArchimedesObjects.blockEngine);
        }
    };

    @Instance(MOD_ID)
    public static ArchimedesShipMod instance;

    @SidedProxy(clientSide = "darkevilmac.archimedes.client.ClientProxy", serverSide = "darkevilmac.archimedes.common.CommonProxy")
    public static CommonProxy proxy;

    public static ArchimedesObjects objects;

    public static Logger modLog;

    public ArchimedesConfig modConfig;
    public NetworkUtil network;

    public ArchimedesShipMod() {
        network = new NetworkUtil();
    }

    @EventHandler
    public void preInitMod(FMLPreInitializationEvent event) {
        modLog = event.getModLog();

        objects = new ArchimedesObjects();

        MinecraftForge.EVENT_BUS.register(this);

        objects.preInit(event);

        modConfig = new ArchimedesConfig(new Configuration(event.getSuggestedConfigurationFile()));
        modConfig.loadAndSave();

    }

    @EventHandler
    public void initMod(FMLInitializationEvent event) {
        modConfig.postLoad();

        try {
            MovingWorld.instance.metaRotations.registerMetaRotationFile("archimedesships.mrot", getClass().getResourceAsStream("/mrot/archimedesships.mrot"));
        } catch (IOException e) {
            modLog.error("UNABLE TO LOAD ARCHIMEDESSHIPS.MROT");
        }

        network.channels = NetworkRegistry.INSTANCE.newChannel
                (MOD_ID, new ArchimedesShipsMessageToMessageCodec(), new ArchimedesShipsPacketHandler());

        objects.init(event);

        MinecraftForge.EVENT_BUS.register(new ConnectionHandler());
        FMLCommonHandler.instance().bus().register(new ConnectionHandler());


        EntityRegistry.registerModEntity(EntityShip.class, "shipmod", 1, this, 64, modConfig.shipEntitySyncRate, true);
        EntityRegistry.registerModEntity(EntitySeat.class, "attachment.seat", 2, this, 64, 100, false);
        EntityRegistry.registerModEntity(EntityParachute.class, "parachute", 3, this, 32, modConfig.shipEntitySyncRate, true);


        proxy.registerKeyHandlers(modConfig);
        proxy.registerEventHandlers();
        proxy.registerRenderers();

        modConfig.addBlacklistWhitelistEntries();
    }

    @EventHandler
    public void postInitMod(FMLPostInitializationEvent event) {
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        registerASCommand(event, new CommandASHelp());
        registerASCommand(event, new CommandDisassembleShip());
        registerASCommand(event, new CommandShipInfo());
        registerASCommand(event, new CommandDisassembleNear());
        Collections.sort(CommandASHelp.asCommands);
    }

    private void registerASCommand(FMLServerStartingEvent event, CommandBase commandbase) {
        event.registerServerCommand(commandbase);
        CommandASHelp.asCommands.add(commandbase);
    }

    private void registerBlock(String id, Block block) {
        registerBlock(id, block, ItemBlock.class);
    }

    private void registerBlock(String id, Block block, Class<? extends ItemBlock> itemblockclass) {
        block.setBlockName("archimedes." + id);
        block.setBlockTextureName("archimedes:" + id);
        GameRegistry.registerBlock(block, itemblockclass, id);
    }

    @Mod.EventHandler
    public void missingMappingFound(FMLMissingMappingsEvent event) {
        if (event != null && event.getAll() != null && !event.getAll().isEmpty()) {
            ListMultimap<String, FMLMissingMappingsEvent.MissingMapping> missing = ReflectionHelper.getPrivateValue(FMLMissingMappingsEvent.class, event, "missing");
            if (missing != null) {
                List<FMLMissingMappingsEvent.MissingMapping> missingMappingList = ImmutableList.copyOf(missing.get("ArchimedesShips"));

                if (missingMappingList != null && !missingMappingList.isEmpty()) {

                    Logger log = LogManager.getLogger(MOD_ID);

                    log.info("ARCHIMEDES LEGACY MAPPINGS FOUND");

                    for (FMLMissingMappingsEvent.MissingMapping mapping : missingMappingList) {
                        if (mapping != null && mapping.type != null && mapping.name != null && !mapping.name.isEmpty()) {

                            String name = mapping.name.substring("ArchimedesShips:".length());

                            if (mapping.type == GameRegistry.Type.BLOCK) {
                                mapping.remap(GameRegistry.findBlock(ArchimedesShipMod.MOD_ID, name));
                            } else {
                                mapping.remap(Item.getItemFromBlock(GameRegistry.findBlock(ArchimedesShipMod.MOD_ID, name)));
                            }

                            log.debug("ArchimedesShips:" + name + " ~~~> " + "ArchimedesShipsPlus:" + name);
                        }
                    }

                    log.info("REMAPPED TO ARCHIMEDES SHIPS PLUS, ENJOY! ~Darkevilmac");
                }
            }
        }
    }

}
