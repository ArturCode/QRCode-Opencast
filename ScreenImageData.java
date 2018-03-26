import java.io.File;
import java.io.IOException;
import java.net.URI;

public class ScreenImageData {

	long timeMilliseconds;
	Attachment attachment;	
	
	public ScreenImageData(long timeMilliseconds, Attachment attachment) {
		this.timeMilliseconds = timeMilliseconds;
		this.attachment = attachment;
	}
	public long getTimeMilliseconds() {
		return timeMilliseconds;
	}
	public void setTimeMilliseconds(long timeMilliseconds) {
		this.timeMilliseconds = timeMilliseconds;
	}
	public Attachment getAttachment() {
		return attachment;
	}
	public void setAttachment(Attachment attachment) {
		this.attachment = attachment;
	}
	
	public File getFile(Workspace workspace) {		
		URI imageUrl = attachment.getURI();
		File imageFile = null;
		
		try {
			imageFile = workspace.get(imageUrl);
		} catch (NotFoundException e) {
			throw new Exception("Image " + imageUrl + " not found in workspace", e);
		} catch (IOException e) {
			throw new Exception("Unable to access " + imageUrl + " in workspace", e);
		}
		
		return imageFile;
	}
}
