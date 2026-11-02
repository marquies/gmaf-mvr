package de.swa.gmaf.plugin;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import de.swa.mmfg.GeneralMetadata;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.builder.FeatureVectorBuilder;
import de.swa.mmfg.builder.JsonFlattener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Exif Plugin based on Apache Commons Imaging
 * 
 * @author stefan_wagenpfeil
 */

public class ExifHandler implements GMAF_Plugin {
	private static final Logger log = LoggerFactory.getLogger(ExifHandler.class);

	/** returns true, if one of the extensions is jpg, jpeg, png, tiff, or gif **/
	public boolean canProcess(String extension) {
		if (extension.equalsIgnoreCase(".jpg")) return true;
		if (extension.equalsIgnoreCase(".jpeg")) return true;
		if (extension.equalsIgnoreCase(".png")) return true;
		if (extension.equalsIgnoreCase(".tiff")) return true;
		if (extension.equalsIgnoreCase(".gif")) return true;
		return false;
	}

	/** extracts a MMFG from the exif data, if it is contained within **/
	public static MMFG getFeatureVectorFromExif(File f) {
		try {
			ImageMetadata imd = Imaging.getMetadata(f);
			if (imd instanceof JpegImageMetadata) {
				JpegImageMetadata jpegMeta = (JpegImageMetadata) imd;
				TiffImageMetadata exif = jpegMeta.getExif();
				TiffOutputSet outputSet = exif.getOutputSet();
				if (outputSet == null)
					outputSet = new TiffOutputSet();
				TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
				TiffOutputField field = exifDirectory.findField(ExifTagConstants.EXIF_TAG_SEMINFO);
				MMFG fv = FeatureVectorBuilder.parse(field.toString());
			}
		} catch (Exception x) {
			log.error("Could not read Exif data", x);
		}
		return null;
	}

	/** attaches a MMFG to the exif metadata **/
	public static void attachFeatureVector(File f, MMFG fv) {
		try {
			ImageMetadata imd = Imaging.getMetadata(f);
			if (imd instanceof JpegImageMetadata) {
				JpegImageMetadata jpegMeta = (JpegImageMetadata) imd;
				TiffImageMetadata exif = jpegMeta.getExif();
				TiffOutputSet outputSet = exif.getOutputSet();
				if (outputSet == null)
					outputSet = new TiffOutputSet();
				TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
				String featureData = FeatureVectorBuilder.flatten(fv, new JsonFlattener());
				exifDirectory.add(ExifTagConstants.EXIF_TAG_SEMINFO, featureData);
				new ExifRewriter().updateExifMetadataLossless(f, new FileOutputStream(f), outputSet);
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	/** processes the asset **/
	public void process(URL url, File f, byte[] bytes, MMFG fv) {
		GeneralMetadata gm = fv.getGeneralMetadata();
		if (gm == null)
			gm = new GeneralMetadata();

		try {
			gm.setFileName(f.getName());
			gm.setFileSize(f.length());
			gm.setFileReference(f);
			
			ImageMetadata imd = Imaging.getMetadata(f);
			if (imd instanceof JpegImageMetadata) {
				JpegImageMetadata jpegMeta = (JpegImageMetadata) imd;
				
				String exif_height = jpegMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_IMAGE_LENGTH).getValueDescription();
				String exif_width = jpegMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_IMAGE_WIDTH).getValueDescription();
				String exif_dpi = jpegMeta.findEXIFValue(TiffTagConstants.TIFF_TAG_IMAGE_WIDTH).getValueDescription();
				String exif_aperture = jpegMeta.findEXIFValue(ExifTagConstants.EXIF_TAG_APERTURE_VALUE).getValueDescription();
				String exif_brightness = jpegMeta.findEXIFValue(ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE).getValueDescription();
				String exif_shutterspeed = jpegMeta.findEXIFValue(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE).getValueDescription();
				String exif_lens = jpegMeta.findEXIFValue(ExifTagConstants.EXIF_TAG_LENS_MODEL).getValueDescription();
				String exif_camera = jpegMeta.findEXIFValue(ExifTagConstants.EXIF_TAG_BODY_SERIAL_NUMBER).getValueDescription();
				String exif_focalLength = jpegMeta.findEXIFValue(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH).getValueDescription();
				String exif_iso = jpegMeta.findEXIFValue(ExifTagConstants.EXIF_TAG_ISO).getValueDescription();
				
				
				if (exif_height != null) gm.setHeight(Integer.parseInt(exif_height));
				if (exif_width != null) gm.setWidth(Integer.parseInt(exif_width));
				if (exif_dpi != null) gm.setResolution(Integer.parseInt(exif_dpi));
				if (exif_aperture != null) gm.setAperture(Float.parseFloat(exif_aperture));
				if (exif_brightness != null) gm.setExposure(Float.parseFloat(exif_brightness));
				if (exif_shutterspeed != null) gm.setShutterSpeed(Long.parseLong(exif_shutterspeed));
				if (exif_lens != null) gm.setLensModel(exif_lens);
				if (exif_camera != null) gm.setCameraModel(exif_camera);
				if (exif_focalLength != null) gm.setFocalLength(Long.parseLong(exif_focalLength));
				if (exif_iso != null) gm.setIso(Integer.parseInt(exif_iso));


				TiffImageMetadata exifMetadata = jpegMeta.getExif();
				if (null != exifMetadata) {
					TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
					if (null != gpsInfo) {
						String gpsDescription = gpsInfo.toString();
						String exif_longitude = jpegMeta.findEXIFValue(GpsTagConstants.GPS_TAG_GPS_LONGITUDE).getValueDescription();
						String exif_latitude = jpegMeta.findEXIFValue(GpsTagConstants.GPS_TAG_GPS_LATITUDE).getValueDescription();

						if (gpsDescription != null) gm.setCityNearBy(gpsDescription);
						if (exif_longitude != null) gm.setLongitude(Long.parseLong(exif_longitude));
						if (exif_latitude != null) gm.setLatitude(Long.parseLong(exif_latitude));
					}
				}
				String dt = jpegMeta.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL).getValueDescription();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
				Date d = sdf.parse(dt);
				gm.setDate(d);
			}
		} catch (Exception x) {
			log.error("Could not process Exif data", x);
		}

		if (gm.getDate() == null) gm.setDate(new Date(f.lastModified()));
		if (gm.getWidth() == 0) {
			Image img = Toolkit.getDefaultToolkit().createImage(bytes);
			gm.setWidth(img.getWidth(null));
			gm.setHeight(img.getHeight(null));
		}
		fv.setGeneralMetadata(gm);
	}

	/** returns always true **/
	public boolean isGeneralPlugin() {
		return true;
	}

	/** returns always false, as exif is not recursive **/
	public boolean providesRecoursiveData() {
		return false;
	}

	public Vector<Node> getDetectedNodes() {
		return null;
	}
}
