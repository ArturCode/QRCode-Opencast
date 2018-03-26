import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class QRCodeFileReader {	

	/** The logging facility */
	private static final Logger logger = LoggerFactory.getLogger(QRCodeFileReader.class);

	public String readQRCode(String fileName) {

		Result result = null;
		BinaryBitmap bitmap = createBitmap(fileName);
		QRCodeReader reader = new QRCodeReader();
		try {
			result = reader.decode(bitmap);
			return result.getText();
		} catch (NotFoundException e) {
			// englisch
			// Die meisten Images enthalten keinen QR-Code, sodass dies der Normalfall ist
			// und keine Exception geworfen wird
		} catch (ChecksumException e) {
			logger.error("barcode was successfully detected of file: {} and decoded, but was not returned because its checksum feature failed: {}", fileName, e);
			throw e;
		} catch (FormatException e) {
			logger.error("Some aspect of the content did not conform to the barcode's format rules {}", e);
			throw e;
		}

		return null;
	}

	private BinaryBitmap createBitmap(String fileName) {

		BinaryBitmap bitmap = null;

		try {
			File file = new File(fileName);
			BufferedImage image = ImageIO.read(file);
			int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
			RGBLuminanceSource source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);
			bitmap = new BinaryBitmap(new HybridBinarizer(source));
		} catch (IllegalArgumentException e) {
			logger.error("Input is null: {}", fileName, e);
			throw e;		
		} catch (IOException e) {
			logger.error("An error occurs during reading of file {}: {}", fileName, e);
			throw e;
		}

		return bitmap;
	}

}
