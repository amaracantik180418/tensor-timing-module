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
