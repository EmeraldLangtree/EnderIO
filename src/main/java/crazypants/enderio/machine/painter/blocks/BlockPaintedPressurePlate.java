package crazypants.enderio.machine.painter.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockPressurePlateWeighted;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.tuple.Pair;

import com.enderio.core.api.client.gui.IResourceTooltipProvider;

import crazypants.enderio.EnderIO;
import crazypants.enderio.EnderIOTab;
import crazypants.enderio.ModObject;
import crazypants.enderio.machine.MachineRecipeRegistry;
import crazypants.enderio.machine.painter.blocks.BlockItemPaintedBlock.INamedSubBlocks;
import crazypants.enderio.machine.painter.recipe.PressurePlatePainterTemplate;
import crazypants.enderio.paint.IPaintable;
import crazypants.enderio.paint.PainterUtil2;
import crazypants.enderio.paint.render.PaintRegistry;
import crazypants.enderio.render.BlockStateWrapper;
import crazypants.enderio.render.EnumRenderPart;
import crazypants.enderio.render.IRenderMapper;
import crazypants.enderio.render.ISmartRenderAwareBlock;
import crazypants.enderio.render.SmartModelAttacher;
import crazypants.enderio.render.dummy.BlockMachineBase;

import static crazypants.util.NbtValue.MOBTYPE;

public class BlockPaintedPressurePlate extends BlockBasePressurePlate implements ITileEntityProvider, IPaintable.ITexturePaintableBlock,
    ISmartRenderAwareBlock, IRenderMapper.IRenderLayerAware, INamedSubBlocks, IResourceTooltipProvider {

  public static class TilePaintedPressurePlate extends TileEntityPaintedBlock {

    private EnumPressurePlateType type = EnumPressurePlateType.WOOD;
    private boolean silent = false;
    private EnumFacing rotation = EnumFacing.NORTH;
    private String mobType = null;

    @Override
    public void readCustomNBT(NBTTagCompound nbtRoot) {
      super.readCustomNBT(nbtRoot);
      byte nbt = nbtRoot.getByte("type");
      type = EnumPressurePlateType.getTypeFromMeta(nbt);
      silent = EnumPressurePlateType.getSilentFromMeta(nbt);
      rotation = EnumFacing.byName(nbtRoot.getString("rotation"));
      if (rotation == null) {
        rotation = EnumFacing.NORTH;
      }
      mobType = MOBTYPE.getString(nbtRoot, null);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbtRoot) {
      super.writeCustomNBT(nbtRoot);
      nbtRoot.setByte("type", (byte) EnumPressurePlateType.getMetaFromType(type, silent));
      nbtRoot.setString("rotation", rotation.getName());
      MOBTYPE.setString(nbtRoot, mobType);
    }

    protected EnumPressurePlateType getType() {
      return type;
    }

    protected void setType(EnumPressurePlateType type) {
      this.type = type;
      markDirty();
    }

    protected boolean isSilent() {
      return silent;
    }

    protected void setSilent(boolean silent) {
      this.silent = silent;
      markDirty();
    }

    protected EnumFacing getRotation() {
      return rotation;
    }

    protected void setRotation(EnumFacing rotation) {
      if (rotation != EnumFacing.DOWN && rotation != EnumFacing.UP) {
        this.rotation = rotation;
        markDirty();
        updateBlock();
      }
    }

    protected String getMobType() {
      return mobType;
    }

    protected void setMobType(String mobType) {
      this.mobType = mobType;
    }

  }

  public static BlockPaintedPressurePlate create() {
    BlockPaintedPressurePlate result = new BlockPaintedPressurePlate(ModObject.blockPaintedPressurePlate.unlocalisedName);
    result.setHardness(0.5F).setStepSound(soundTypeWood);
    result.init();
    MachineRecipeRegistry.instance.registerRecipe(ModObject.blockPainter.unlocalisedName,
        new PressurePlatePainterTemplate(result, EnumPressurePlateType.WOOD.getMetaFromType(), Blocks.wooden_pressure_plate));
    MachineRecipeRegistry.instance.registerRecipe(ModObject.blockPainter.unlocalisedName,
        new PressurePlatePainterTemplate(result, EnumPressurePlateType.STONE.getMetaFromType(), Blocks.stone_pressure_plate));
    MachineRecipeRegistry.instance.registerRecipe(ModObject.blockPainter.unlocalisedName,
        new PressurePlatePainterTemplate(result, EnumPressurePlateType.IRON.getMetaFromType(), Blocks.heavy_weighted_pressure_plate));
    MachineRecipeRegistry.instance.registerRecipe(ModObject.blockPainter.unlocalisedName,
        new PressurePlatePainterTemplate(result, EnumPressurePlateType.GOLD.getMetaFromType(), Blocks.light_weighted_pressure_plate));

    // SpecialTooltipHandler.INSTANCE.addCallback(new SoulToolTip());

    return result;
  }

  private final String name;

  public BlockPaintedPressurePlate(String name) {
    super(Material.iron);
    this.setDefaultState(this.blockState.getBaseState().withProperty(BlockPressurePlateWeighted.POWER, 0));
    setCreativeTab(EnderIOTab.tabEnderIO);
    this.name = name;
    setUnlocalizedName(name);
  }

  private final IBlockState[] defaultPaints = new IBlockState[EnumPressurePlateType.values().length];

  private void init() {
    GameRegistry.registerBlock(this, null, name);
    GameRegistry.registerItem(new BlockItemPaintedPressurePlate(this), name);
    GameRegistry.registerTileEntity(TilePaintedPressurePlate.class, name + "TileEntity");
    SmartModelAttacher.registerNoProps(this);
    PaintRegistry.registerModel("pressure_plate_up", new ResourceLocation("minecraft", "block/stone_pressure_plate_up"), PaintRegistry.PaintMode.ALL_TEXTURES);
    PaintRegistry.registerModel("pressure_plate_down", new ResourceLocation("minecraft", "block/stone_pressure_plate_down"),
        PaintRegistry.PaintMode.ALL_TEXTURES);
    PaintRegistry.registerModel("pressure_plate_inventory", new ResourceLocation("minecraft", "block/stone_pressure_plate_inventory"),
        PaintRegistry.PaintMode.ALL_TEXTURES);

    defaultPaints[EnumPressurePlateType.WOOD.ordinal()] = Blocks.wooden_pressure_plate.getDefaultState();
    defaultPaints[EnumPressurePlateType.STONE.ordinal()] = Blocks.stone_pressure_plate.getDefaultState();
    defaultPaints[EnumPressurePlateType.IRON.ordinal()] = Blocks.heavy_weighted_pressure_plate.getDefaultState();
    defaultPaints[EnumPressurePlateType.GOLD.ordinal()] = Blocks.light_weighted_pressure_plate.getDefaultState();
    defaultPaints[EnumPressurePlateType.DARKSTEEL.ordinal()] = getDefaultState().withProperty(BlockPressurePlateWeighted.POWER, 1);
    defaultPaints[EnumPressurePlateType.SOULARIUM.ordinal()] = getDefaultState().withProperty(BlockPressurePlateWeighted.POWER, 2);
    defaultPaints[EnumPressurePlateType.TUNED.ordinal()] = getDefaultState().withProperty(BlockPressurePlateWeighted.POWER, 3);
  }

  @Override
  public TileEntity createNewTileEntity(World world, int metadata) {
    return new TilePaintedPressurePlate();
  }

  @Override
  public IBlockState getStateFromMeta(int meta) {
    return this.getDefaultState().withProperty(BlockPressurePlateWeighted.POWER, meta);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(BlockPressurePlateWeighted.POWER);
  }

  @Override
  protected BlockState createBlockState() {
    return new BlockState(this, new IProperty[] { BlockPressurePlateWeighted.POWER });
  }

  @Override
  protected int computeRedstoneStrength(World worldIn, BlockPos pos) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      EnumPressurePlateType type = ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).getType();
      return type.getCountingMode().count(
          worldIn.getEntitiesWithinAABB(type.getSearchClass(), this.getSensitiveAABB(pos), type.getPredicate(getMobType(worldIn, pos))));
    } else {
      return getRedstoneStrength(worldIn.getBlockState(pos));
    }
  }

  @Override
  protected int getRedstoneStrength(IBlockState state) {
    return state.getValue(BlockPressurePlateWeighted.POWER);
  }

  @Override
  protected IBlockState setRedstoneStrength(IBlockState state, int strength) {
    return state.withProperty(BlockPressurePlateWeighted.POWER, strength);
  }

  protected void setTypeFromMeta(IBlockAccess worldIn, BlockPos pos, int meta) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).setType(EnumPressurePlateType.getTypeFromMeta(meta));
      ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).setSilent(EnumPressurePlateType.getSilentFromMeta(meta));
    }
  }

  protected int getMetaForStack(IBlockAccess worldIn, BlockPos pos) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      return EnumPressurePlateType.getMetaFromType(((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).getType(),
          ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).isSilent());
    }
    return 0;
  }

  protected EnumPressurePlateType getType(IBlockAccess worldIn, BlockPos pos) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      return ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).getType();
    }
    return EnumPressurePlateType.WOOD;
  }

  protected boolean isSilent(IBlockAccess worldIn, BlockPos pos) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      return ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).isSilent();
    }
    return false;
  }

  protected String getMobType(IBlockAccess worldIn, BlockPos pos) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      return ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).getMobType();
    }
    return null;
  }

  protected void setMobType(IBlockAccess worldIn, BlockPos pos, String mobType) {
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).setMobType(mobType);
    }
  }

  @Override
  public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState();
  }

  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
    setTypeFromMeta(worldIn, pos, stack.getMetadata());
    setPaintSource(state, worldIn, pos, PainterUtil2.getSourceBlock(stack));
    setRotation(worldIn, pos, EnumFacing.fromAngle(placer.rotationYaw));
    setMobType(worldIn, pos, MOBTYPE.getString(stack));
    if (!worldIn.isRemote) {
      worldIn.markBlockForUpdate(pos);
    }
  }

  @Override
  public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
    setRotation(world, pos, getRotation(world, pos).rotateAround(EnumFacing.Axis.Y));
    return true;
  }

  @Override
  public boolean removedByPlayer(World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
    if (willHarvest) {
      return true;
    }
    return super.removedByPlayer(world, pos, player, willHarvest);
  }

  @Override
  public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
    super.harvestBlock(worldIn, player, pos, state, te);
    super.removedByPlayer(worldIn, pos, player, true);
  }

  @Override
  public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
    return Collections.singletonList(getDrop(world, pos));
  }

  protected ItemStack getDrop(IBlockAccess world, BlockPos pos) {
    ItemStack drop = new ItemStack(Item.getItemFromBlock(this), 1, getMetaForStack(world, pos));
    PainterUtil2.setSourceBlock(drop, getPaintSource(null, world, pos));
    MOBTYPE.setString(drop, getMobType(world, pos));
    return drop;
  }

  @Override
  public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos, EntityPlayer player) {
    return getDrop(world, pos);
  }

  @Override
  public void setPaintSource(IBlockState state, IBlockAccess world, BlockPos pos, IBlockState paintSource) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof IPaintable.IPaintableTileEntity) {
      if (defaultPaints[getType(world, pos).ordinal()] == paintSource) {
        ((IPaintableTileEntity) te).setPaintSource(null);
      } else {
        ((IPaintableTileEntity) te).setPaintSource(paintSource);
      }
    }
  }

  @Override
  public void setPaintSource(Block block, ItemStack stack, IBlockState paintSource) {
    if (defaultPaints[EnumPressurePlateType.getTypeFromMeta(stack.getMetadata()).ordinal()] == paintSource) {
      PainterUtil2.setSourceBlock(stack, null);
    } else {
      PainterUtil2.setSourceBlock(stack, paintSource);
    }
  }

  @Override
  public IBlockState getPaintSource(IBlockState state, IBlockAccess world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof IPaintable.IPaintableTileEntity) {
      IBlockState paintSource = ((IPaintableTileEntity) te).getPaintSource();
      if (paintSource != null) {
        return paintSource;
      }
    }
    return defaultPaints[getType(world, pos).ordinal()];
  }

  @Override
  public IBlockState getPaintSource(Block block, ItemStack stack) {
    IBlockState paintSource = PainterUtil2.getSourceBlock(stack);
    return paintSource != null ? paintSource : defaultPaints[EnumPressurePlateType.getTypeFromMeta(stack.getMetadata()).ordinal()];
  }

  @Override
  public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
    return new BlockStateWrapper(state, world, pos);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public IRenderMapper getRenderMapper() {
    return this;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public Pair<List<IBlockState>, List<IBakedModel>> mapBlockRender(BlockStateWrapper state, IBlockAccess world, BlockPos pos) {
    IBlockState paintSource = getPaintSource(state, world, pos);
    if (paintSource != null && paintSource.getBlock().canRenderInLayer(MinecraftForgeClient.getRenderLayer())
        && paintSource.getBlock() != EnderIO.blockFusedQuartz) {
      return Pair.of(null, Collections.singletonList(mapRender(state, paintSource, getRotation(world, pos))));
    } else {
      return null;
    }
  }

  @SideOnly(Side.CLIENT)
  private IBakedModel mapRender(IBlockState state, IBlockState paint, EnumFacing facing) {

    ModelRotation rot;
    switch (facing) {
    case EAST:
      rot = ModelRotation.X0_Y90;
      break;
    case NORTH:
      rot = null;
      break;
    case SOUTH:
      rot = ModelRotation.X0_Y180;
      break;
    case WEST:
      rot = ModelRotation.X0_Y270;
      break;
    default:
      return null;
    }

    if (state.getValue(BlockPressurePlateWeighted.POWER) > 0) {
      return PaintRegistry.getModel(IBakedModel.class, "pressure_plate_down", paint, rot);
    } else {
      return PaintRegistry.getModel(IBakedModel.class, "pressure_plate_up", paint, rot);
    }
  }

  protected EnumFacing getRotation(IBlockAccess world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TilePaintedPressurePlate) {
      return ((TilePaintedPressurePlate) te).getRotation();
    }
    return EnumFacing.NORTH;
  }

  protected void setRotation(IBlockAccess world, BlockPos pos, EnumFacing rotation) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TilePaintedPressurePlate) {
      ((TilePaintedPressurePlate) te).setRotation(rotation);
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public List<IBlockState> mapOverlayLayer(BlockStateWrapper state, IBlockAccess world, BlockPos pos) {
    return null;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public Pair<List<IBlockState>, List<IBakedModel>> mapItemRender(Block block, ItemStack stack) {
    IBlockState paintSource = getPaintSource(block, stack);
    if (paintSource != null) {
      IBlockState stdOverlay = BlockMachineBase.block.getDefaultState().withProperty(EnumRenderPart.SUB, EnumRenderPart.PAINT_OVERLAY);
      IBakedModel model1 = PaintRegistry.getModel(IBakedModel.class, "pressure_plate_inventory", paintSource, null);
      List<IBakedModel> list = new ArrayList<IBakedModel>();
      list.add(model1);
      if (paintSource != defaultPaints[EnumPressurePlateType.getTypeFromMeta(stack.getMetadata()).ordinal()]) {
        IBakedModel model2 = PaintRegistry.getModel(IBakedModel.class, "pressure_plate_inventory", stdOverlay, PaintRegistry.OVERLAY_TRANSFORMATION);
        list.add(model2);
      }
      return Pair.of(null, list);
    } else {
      return null;
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public Pair<List<IBlockState>, List<IBakedModel>> mapItemPaintOverlayRender(Block block, ItemStack stack) {
    return null;
  }

  @Override
  public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
    return true;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public int colorMultiplier(IBlockAccess worldIn, BlockPos pos, int renderPass) {
    IBlockState paintSource = getPaintSource(worldIn.getBlockState(pos), worldIn, pos);
    if (paintSource != null) {
      try {
        return paintSource.getBlock().colorMultiplier(worldIn, pos, renderPass);
      } catch (Throwable e) {
      }
    }
    return super.colorMultiplier(worldIn, pos, renderPass);
  }

  @Override
  public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      return EnumPressurePlateType.WOOD == ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).getType() ? 20 : 0;
    }
    return 0;
  }

  @Override
  public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof BlockPaintedPressurePlate.TilePaintedPressurePlate) {
      return EnumPressurePlateType.WOOD == ((BlockPaintedPressurePlate.TilePaintedPressurePlate) te).getType() ? 5 : 0;
    }
    return 0;
  }

  @Override
  public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
    for (EnumPressurePlateType type : EnumPressurePlateType.values()) {
      if (tab == EnderIOTab.tabNoTab || type.ordinal() >= EnumPressurePlateType.DARKSTEEL.ordinal()) {
        list.add(new ItemStack(itemIn, 1, EnumPressurePlateType.getMetaFromType(type, false)));
      }
      list.add(new ItemStack(itemIn, 1, EnumPressurePlateType.getMetaFromType(type, true)));
    }
  }

  @Override
  protected void updateState(World worldIn, BlockPos pos, IBlockState state, int oldRedstoneStrength) {
    int newRedstoneStrength = this.computeRedstoneStrength(worldIn, pos);
    boolean wasOn = oldRedstoneStrength > 0;
    boolean isOn = newRedstoneStrength > 0;

    if (oldRedstoneStrength != newRedstoneStrength) {
      state = this.setRedstoneStrength(state, newRedstoneStrength);
      worldIn.setBlockState(pos, state, 2);
      this.updateNeighbors(worldIn, pos);
      worldIn.markBlockRangeForRenderUpdate(pos, pos);

      if (!isSilent(worldIn, pos)) {
        if (!isOn && wasOn) {
          worldIn.playSoundEffect(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, "random.click", 0.3F, 0.5F);
        } else if (isOn && !wasOn) {
          worldIn.playSoundEffect(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, "random.click", 0.3F, 0.6F);
        }
      }
    }

    if (isOn) {
      worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
    }
  }

  public static class BlockItemPaintedPressurePlate extends BlockItemPaintedBlock {

    public BlockItemPaintedPressurePlate(BlockPaintedPressurePlate block) {
      super(block);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
      return EnumPressurePlateType.getTypeFromMeta(stack.getMetadata()) == EnumPressurePlateType.TUNED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
      super.addInformation(stack, playerIn, tooltip, advanced);
      if (hasMob(stack)) {
        tooltip.add(EnderIO.lang.localize("tile.plockPaintedPressurePlate.tuned",
            StatCollector.translateToLocal("entity." + MOBTYPE.getString(stack) + ".name")));
      }
    }

    private boolean hasMob(ItemStack stack) {
      return stack != null && EnumPressurePlateType.getTypeFromMeta(stack.getMetadata()) == EnumPressurePlateType.TUNED && MOBTYPE.hasTag(stack);
    }

  }

  @Override
  public String getUnlocalizedName(int meta) {
    return getUnlocalizedName() + "." + EnumPressurePlateType.getTypeFromMeta(meta).getName()
        + (EnumPressurePlateType.getSilentFromMeta(meta) ? ".silent" : "");
  }

  @Override
  public String getUnlocalizedNameForTooltip(ItemStack itemStack) {
    return getUnlocalizedName(itemStack.getMetadata());
  }

}
