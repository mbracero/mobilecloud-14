package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class MyController {
	
	private Collection<Video> videos = new CopyOnWriteArrayList<Video>();

	private static final AtomicLong currentId = new AtomicLong(0L);
	private static final int CODE_STATUS_NOT_FOUND = 404;
	
	/**
	 * GET /video
	 * 
	 * Returns the list of videos that have been added to the server as JSON. The list of videos does not have
	 * to be persisted across restarts of the server. The list of Video objects should be able to be unmarshalled
	 * by the client into a Collection.
	 * 
	 * The return content-type should be application/json, which will be the default if you use @ResponseBody
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideos() {
		return videos;
	}
	
	/**
	 * POST /video
	 * 
	 * The video metadata is provided as an application/json request body. The JSON should generate a valid instance
	 * of the Video class when deserialized by Spring's default Jackson library.
	 * 
	 * Returns the JSON representation of the Video object that was stored along with any updates to that object made by the server.
	 * 
	 * The server should generate a unique identifier for the Video object and assign it to the Video by calling its setId(...) method.
	 * 
	 * No video should have ID = 0. All IDs should be > 0.
	 * 
	 * The returned Video JSON should include this server-generated identifier so that the client can refer to it when uploading the
	 * binary mpeg video content for the Video.
	 * 
	 * The server should also generate a "data url" for the Video. The "data url" is the url of the binary data for a Video (e.g., the raw
	 * mpeg data). The URL should be the full URL for the video and not just the path (e.g., http://localhost:8080/video/1/data would be a
	 * valid data url). See the Hints section for some ideas on how to generate this URL.
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		checkAndSetId(v);
		v.setDataUrl(getDataUrl(v.getId()));
		videos.add(v);
		
		return v;
	}
	
    /**
     * POST /video/{id}/data
     * 
     * The binary mpeg data for the video should be provided in a multipart request as a part with the key "data". The id in the path
     * should be replaced with the unique identifier generated by the server for the Video. A client MUST create a Video first by sending
     * a POST to /video and getting the identifier for the newly created Video object before sending a POST to /video/{id}/data.
     * 
     * The endpoint should return a VideoStatus object with state=VideoState.READY if the request succeeds and the appropriate HTTP error
     * status otherwise. VideoState.PROCESSING is not used in this assignment but is present in VideoState.
     * 
     * Rather than a PUT request, a POST is used because, by default, Spring does not support a PUT with multipart data due to design decisions
     * in the Commons File Upload library: https://issues.apache.org/jira/browse/FILEUPLOAD-197
     */
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable(value=VideoSvcApi.ID_PARAMETER) long id,
			@RequestParam(value=VideoSvcApi.DATA_PARAMETER) MultipartFile videoData,
			HttpServletResponse response /**
										 * Any Controller method can take an HttpServletRequest or HttpServletResponse as parameters to gain
										 * low-level access/control over the HTTP messages. Spring will automatically fill in these parameters
										 * when your Controller's method is invoked.
										 * 
										 * Maybe you want to set the status code with the response
										 * or write some binary data to an OutputStream obtained from
										 * the HttpServletResponse object
										 */
	) throws IOException {
    	try {
    		InputStream inputStream = videoData.getInputStream();
	    	
	    	Video video = null;
	    	for (Video v : videos) {
				if (id == v.getId()) {
					video = v;
				}
			}
	    	
	    	VideoFileManager videoFileManager = VideoFileManager.get();
	    	videoFileManager.saveVideoData(video, inputStream);
    	} catch (Exception e) {
    		response.sendError(CODE_STATUS_NOT_FOUND);
		}
    	
    	VideoStatus videoStatus = new VideoStatus(VideoState.READY);
    	return videoStatus;
    }
    
    /**
     * GET /video/{id}/data
     * 
     * Returns the binary mpeg data (if any) for the video with the given identifier. If no mpeg data has been uploaded for the specified video, then
     * the server should return a 404 status code.
     */
    @RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
    public @ResponseBody void getData(
    		@PathVariable(value=VideoSvcApi.ID_PARAMETER) long id,
    		HttpServletResponse response /**
										 * Any Controller method can take an HttpServletRequest or HttpServletResponse as parameters to gain
										 * low-level access/control over the HTTP messages. Spring will automatically fill in these parameters
										 * when your Controller's method is invoked.
										 * 
										 * Maybe you want to set the status code with the response
										 * or write some binary data to an OutputStream obtained from
										 * the HttpServletResponse object
										 */
    ) throws IOException {
    	try {
	    	Video video = null;
	    	for (Video v : videos) {
				if (id == v.getId()) {
					video = v;
				}
			}
	    	
	    	VideoFileManager videoFileManager = VideoFileManager.get();
    		videoFileManager.copyVideoData(video, response.getOutputStream());
    	} catch (Exception e) {
    		response.sendError(CODE_STATUS_NOT_FOUND);
		}
    }
    
    /**
     * UTILS
     */
	private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }
	
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
       HttpServletRequest request = 
           ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
       String base = 
          "http://"+request.getServerName() 
          + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
       return base;
    }
}
