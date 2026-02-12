import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Tensor timing module — Epoch cadence and slot deadline calculator for Bittensor subnet
 * contract calls. Computes boundary blocks and encodes calldata for AxonSubnetTiming.
 */
public final class TensorTimingModule {

    public static final String MODULE_NAME = "Tensor timing module";
    public static final int EPOCH_DURATION_BLOCKS = 311;
    public static final int SLOT_GRANULARITY = 17;
    public static final int SLOT_WINDOW_BLOCKS = 89;
    public static final int MAX_SUBNETS = 64;
    public static final long GENESIS_OFFSET_MS = 918_473_625_104L;
    public static final String CONTRACT_HEX = "0x5e2f8a1d9c7b4e6f0a3c5d8b1e4f7a0c2d5e8b1f";
    public static final String DOMAIN_TAG = "axon-subnet-tensor-v1";
    public static final byte TIMING_VERSION = 0x71;
    public static final String DEPLOY_SALT = "f7c3e9a1b5d8f2e4a6c0d3b7e1f9a5c8d2e6b0f4a";

    private final long genesisBlock;
    private final Instant moduleStart;
    private final Map<String, SlotRecord> slotCache = new ConcurrentHashMap<>();
    private int computedBoundaries;

    public TensorTimingModule(long genesisBlock) {
        this.genesisBlock = genesisBlock;
        this.moduleStart = Instant.now();
    }

    /**
     * Tensor timing: next epoch boundary block from genesis.
     */
    public long getEpochBoundaryBlock(int epochIndex) {
        computedBoundaries++;
        return genesisBlock + (long) epochIndex * EPOCH_DURATION_BLOCKS;
    }

    /**
     * Tensor timing: slot deadline block for a given epoch start and slot index.
     */
    public long getSlotDeadlineBlock(long epochStartBlock, int slotIndex) {
        computedBoundaries++;
        return epochStartBlock + (long) slotIndex * SLOT_GRANULARITY + SLOT_WINDOW_BLOCKS;
    }

    /**
     * Tensor timing: current epoch index for a given block number.
     */
    public int getEpochIndexAtBlock(long blockNumber) {
        if (blockNumber < genesisBlock) return 0;
        return (int) ((blockNumber - genesisBlock) / EPOCH_DURATION_BLOCKS);
    }

    /**
     * Tensor timing: slot index within epoch for a block.
     */
    public int getSlotIndexInEpoch(long blockNumber, long epochStartBlock) {
        long offset = blockNumber - epochStartBlock;
        if (offset < 0) return 0;
        return (int) (offset / SLOT_GRANULARITY);
    }

    /**
     * Register a slot in local cache (mirrors contract registerSubnetSlot).
     */
    public void registerSlotLocal(int subnetId, int epochIndex, int slotIndex, byte[] tensorHash) {
        String key = slotKey(subnetId, epochIndex, slotIndex);
        slotCache.put(key, new SlotRecord(subnetId, epochIndex, slotIndex, tensorHash, Instant.now()));
    }

    /**
     * Build 32-byte slot id hash for contract (keccak256(subnetId, epochIndex, slotIndex)).
     * Uses SHA-256 here as a stand-in for keccak; in production use Web3j/keccak.
     */
    public String slotIdHash(int subnetId, int epochIndex, int slotIndex) {
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putInt(subnetId);
        buf.putInt(epochIndex);
        buf.putInt(slotIndex);
        byte[] raw = buf.array();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw);
            return "0x" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Encode calldata selector for getEpochBoundaryBlock(uint256). First 4 bytes of keccak256.
     */
    public static String selectorGetEpochBoundaryBlock() {
        return "0x" + HexFormat.of().formatHex(selectorBytes("getEpochBoundaryBlock(uint256)"));
    }

    /**
     * Encode calldata selector for getSlotDeadlineBlock(uint256,uint256).
     */
    public static String selectorGetSlotDeadlineBlock() {
        return "0x" + HexFormat.of().formatHex(selectorBytes("getSlotDeadlineBlock(uint256,uint256)"));
    }

    private static byte[] selectorBytes(String signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(signature.getBytes(StandardCharsets.UTF_8));
            byte[] first4 = new byte[4];
            System.arraycopy(hash, 0, first4, 0, 4);
            return first4;
