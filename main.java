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
