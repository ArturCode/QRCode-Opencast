package org.opencastproject.workflow.handler.qrcodeanalyzer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.mpeg7.MediaDuration;
import org.opencastproject.metadata.mpeg7.MediaRelTimePointImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition;
import org.opencastproject.metadata.mpeg7.SpatioTemporalLocator;
import org.opencastproject.metadata.mpeg7.SpatioTemporalLocatorImpl;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.metadata.mpeg7.VideoSegment;
import org.opencastproject.metadata.mpeg7.VideoText;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.textanalyzer.api.TextAnalyzerException;
import org.opencastproject.textanalyzer.api.TextAnalyzerService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

public class QRCodeAnalysisWorkflowOperationHandler {

	/** The logging facility */
	private static final Logger logger = LoggerFactory.getLogger(QRCodeAnalysisWorkflowOperationHandler.class);

	protected WorkflowOperationResult extractQRCode(final MediaPackage mediaPackage)
			throws EncoderException, TextAnalyzerException, WorkflowOperationException, IOException, NotFoundException {

		List<ScreenImageData> screenImages;
		try {
			long extractImageIntervalMilliseconds = 500;
			screenImages = extractImages(mediaPackage, extractImageIntervalMilliseconds);

			for (ScreenImageData image : screenImages) {
				File imageFile = image.GetFile(workspace);
				String filePath = imageFile.getPath();

				QRCodeFileReader qrFileReader = new QRCodeFileReader();
				String qrcode = qrFileReader.readQRCode(filePath);
				QRCodeParser parser = new QRCodeParser(qrcode);
				String guid = parser.getGuid();
				if (guid != null) {
					String videoID = mediaPackage.getIdentifier().toString();
					long time = image.getTimeSeconds();
					QRCodeDataAccess qrDataAccess = new QRCodeDataAccess();
					qrDataAccess.insert(guid, time, videoID);
					// System.out.println("QR-Code: " + guid + " from file: " + filePath);
				}
			}
		} catch (EncoderException e) {
			logger.error("Error creating still image(s) from {}", mediaPackage);
			throw e;
		} finally {
			// Remove images that were created for QR-Code extraction
			logger.debug("Removing temporary imagesQr");
			for (ScreenImageData image : screenImages) {
				try {
					workspace.delete(image.getAttachment().getURI());
				} catch (Exception e) {
					logger.warn("Unable to delete temporary image {}: {}", image.getAttachment().getURI(), e);
				}
			}
		}
	}

	private List<ScreenImageData> extractImages(final MediaPackage mediaPackage, int intervalMilliseconds)
			throws EncoderException {

		Track[] tracks = mediaPackage.getTracks();
		Track sourceTrack = tracks[0];
		long videoDuration = sourceTrack.getDuration();
		List<ScreenImageData> screenImages = new LinkedList<ScreenImageData>();
		SortedMap<Long, Job> extractImageJobsQr = new TreeMap<Long, Job>();
		System.out.println("sourceTrack: " + sourceTrack + "videoDuration: " + videoDuration);

		for (long i = 0; i < videoDuration; i += intervalMilliseconds) {
			double time = (double) i / 1000;
			Job imageExtractionJob = composer.image(sourceTrack, IMAGE_EXTRACTION_PROFILE, time);
			extractImageJobsQr.put(i, imageExtractionJob);
			// System.out.println("Zeit: " + time);
		}

		if (!waitForStatus(extractImageJobsQr.values().toArray(new Job[extractImageJobsQr.size()])).isSuccess())
			throw new WorkflowOperationException("Extracting scene image from " + sourceTrack + " failed");

		for (Map.Entry<Long, Job> entry : extractImageJobsQr.entrySet()) {
			Job job = serviceRegistry.getJob(entry.getValue().getId());
			Attachment image = (Attachment) MediaPackageElementParser.getFromXml(job.getPayload());

			ScreenImageData screenImageData = new ScreenImageData(entry.getKey(), image);
			screenImages.add(screenImageData);
		}

		return screenImages;
	}
}