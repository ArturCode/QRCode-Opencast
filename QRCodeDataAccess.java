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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

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

public class QRCodeDataAccess {

	/** The logging facility */
	private static final Logger logger = LoggerFactory.getLogger(QRCodeDataAccess.class);

	public void insert(String guid, long timeMilliseconds, String videoID) {

		// In Configuration auslagern
		String url = "jdbc:mysql://localhost:3306/";
		String user = "opencast";
		String password = "opencast_password";
		String tableName = "datenbankRed"; // Tabellennamen ändern beispielsweise "qrcode_screen_datas"
		String databaseName = "opencast";

		Connection con = null;
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection(url, user, password);

			if (con == null) {
				String pwHint = null;
				if (password == null || password.isEmpty()) {
					pwHint = "Password is NOT set.";
				} else {
					pwHint = "Password is set.";
				}
				string errorMessage = "Unable to connect to database " + url + " with user " + user + ". " + pwHint;
				logger.error(errorMessage);
				throw new Exception(errorMessage);
			}

			long timeSeconds = timeMilliseconds / 1000;

			Statement stt = con.createStatement();
			stt.execute("USE " + databaseName);
			stt.execute("INSERT INTO " + tableName + " (guid, timeMilliseconds, videoID) SELECT '" + guid + "', '"
					+ timeSeconds + "', '" + videoID + "' WHERE NOT EXISTS(SELECT * FROM " + tableName + " WHERE guid = '"
					+ guid + "')");
			stt.close();
			con.close();
		} catch (SQLException e) {
			logger.error(
					"An error occurs while inserting into databese URL: {} USER: {} with GUID: {} timeMilliseconds: {} VideoID: {}: {}",
					url, user, guid, timeMilliseconds, videoID, e);
			throw e;
		} catch (Exception e) {
			logger.error("Error while accessing databse with GUID: {} timeMilliseconds: {} VideoID: {}: {}", guid,
					timeMilliseconds, videoID, e);
			throw e;
		} finally {
			try {
				if (con != null) {
					con.close();
				}
			} catch (SQLException e) {
				logger.error(
						"An error occurs while inserting into databese URL: {} USER: {} with GUID: {} timeMilliseconds: {} VideoID: {}: {}",
						url, user, guid, timeMilliseconds, videoID, e);
				throw e;
			}
		}
	}
}