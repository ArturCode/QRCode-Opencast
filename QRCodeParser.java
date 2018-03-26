import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class QRCodeParser {

	/** The logging facility */
	private static final Logger logger = LoggerFactory.getLogger(QRCodeParser.class);
	
	private String qrcode;	
	
	public QRCodeParser(String qrcode) {
		this.qrcode = qrcode;
	}

	public String getGuid() {
		
		if (!isAvailable()) {
			return null;
		}		
		
		String guid = null;
		
		try {
			URL aURL = new URL(qrcode);
			guid = splitQuery(aURL).get("guid");
		} catch (UnsupportedEncodingException e) {
			logger.error("Error while splitting URL: {}: {}", aURL, e);
		} catch (MalformedURLException e) {
			logger.error("No protocol is specified, or an unknown protocol is found, or spec is null: {}", e);
		}

		return guid;
	}
	
	private static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
		Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		String query = url.getQuery();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		}
		return query_pairs;
	}
	
	private boolean isAvailable() {
		return qrcode != null && !qrcode.isEmpty();
	}
}
