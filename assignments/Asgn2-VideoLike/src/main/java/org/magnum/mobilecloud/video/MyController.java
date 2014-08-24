package org.magnum.mobilecloud.video;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MyController {
	private static final int CODE_STATUS_OK = 200;
	private static final int CODE_STATUS_BAD_REQUEST = 400;
	private static final int CODE_STATUS_NOT_FOUND = 404;
	
	/**
	 * Dependency injection to JPA repository
	 */
	@Autowired
	private VideoRepository videoRepo;
	
	/**
	 * GET /video
	 *    - Returns the list of videos that have been added to the
	 *      server as JSON. The list of videos should be persisted
	 *      using Spring Data. The list of Video objects should be able 
	 *      to be unmarshalled by the client into a Collection<Video>.
	 *    - The return content-type should be application/json, which
	 *      will be the default if you use @ResponseBody
	 * 
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		Collection<Video> ret = (Collection<Video>) videoRepo.findAll();
		return ret;
	}
	
	/**
	 * POST /video
	 *    - The video metadata is provided as an application/json request
	 *      body. The JSON should generate a valid instance of the 
	 *      Video class when deserialized by Spring's default 
	 *      Jackson library.
	 *    - Returns the JSON representation of the Video object that
	 *      was stored along with any updates to that object made by the server. 
	 *    - **_The server should store the Video in a Spring Data JPA repository.
	 *    	 If done properly, the repository should handle generating ID's._** 
	 *    - A video should not have any likes when it is initially created.
	 *    - You will need to add one or more annotations to the Video object
	 *      in order for it to be persisted with JPA.
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		Video ret = videoRepo.save(v);
		return ret;
	}
	
	/** 
	 * GET /video/{id}
	 *    - Returns the video with the given id or 404 if the video is not found.
	 * 
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}", method=RequestMethod.GET)
	public @ResponseBody Video getVideoById(
			@PathVariable("id") long id,
    		HttpServletResponse response /**
			 * Any Controller method can take an HttpServletRequest or HttpServletResponse as parameters to gain
			 * low-level access/control over the HTTP messages. Spring will automatically fill in these parameters
			 * when your Controller's method is invoked.
			 * 
			 * Maybe you want to set the status code with the response
			 * or write some binary data to an OutputStream obtained from
			 * the HttpServletResponse object
			 */) {
		Video ret = videoRepo.findOne(id);
		if (ret == null) { response.setStatus(CODE_STATUS_NOT_FOUND); }
		return ret;
	}
	
	/**
	 * POST /video/{id}/like
	 *    - Allows a user to like a video. Returns 200 Ok on success, 404 if the
	 *      video is not found, or 400 if the user has already liked the video.
	 *    - The service should should keep track of which users have liked a video and
	 *      prevent a user from liking a video twice. A POJO Video object is provided for 
	 *      you and you will need to annotate and/or add to it in order to make it persistable.
	 *    - A user is only allowed to like a video once. If a user tries to like a video
	 *       a second time, the operation should fail and return 400 Bad Request.
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method=RequestMethod.POST)
	public void likeVideo(
			@PathVariable("id") long id,
			Principal principal, /**
			 * Any Controller method can take a Principal as a parameter to gain access/control
			 * over the user who is currently authenticated. Spring will automatically fill in this
			 * parameter when your Controller's method is invoked
			 */
    		HttpServletResponse response /**
			 * Any Controller method can take an HttpServletRequest or HttpServletResponse as parameters to gain
			 * low-level access/control over the HTTP messages. Spring will automatically fill in these parameters
			 * when your Controller's method is invoked.
			 * 
			 * Maybe you want to set the status code with the response
			 * or write some binary data to an OutputStream obtained from
			 * the HttpServletResponse object
			 */) {
		Video video = videoRepo.findOne(id);
		if (video == null) { response.setStatus(CODE_STATUS_NOT_FOUND); }
		else if ( video.getLikesUsers().contains( principal.getName() ) ) { response.setStatus(CODE_STATUS_BAD_REQUEST); }
		else {
			video.setLikes( video.getLikes() + 1 ); // Increment likes
			video.getLikesUsers().add( principal.getName() ); // Add user name
			
			videoRepo.save(video); // persist the object
			
			response.setStatus(CODE_STATUS_OK);
		}
	}
	
	/**
	 * POST /video/{id}/unlike
	 *    - Allows a user to unlike a video that he/she previously liked. Returns 200 OK
	 *       on success, 404 if the video is not found, and a 400 if the user has not 
	 *       previously liked the specified video.
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method=RequestMethod.POST)
	public void unlikeVideo(
			@PathVariable("id") long id,
			Principal principal, /**
			 * Any Controller method can take a Principal as a parameter to gain access/control
			 * over the user who is currently authenticated. Spring will automatically fill in this
			 * parameter when your Controller's method is invoked
			 */
    		HttpServletResponse response /**
			 * Any Controller method can take an HttpServletRequest or HttpServletResponse as parameters to gain
			 * low-level access/control over the HTTP messages. Spring will automatically fill in these parameters
			 * when your Controller's method is invoked.
			 * 
			 * Maybe you want to set the status code with the response
			 * or write some binary data to an OutputStream obtained from
			 * the HttpServletResponse object
			 */) {
		Video video = videoRepo.findOne(id);
		if (video == null) { response.setStatus(CODE_STATUS_NOT_FOUND); }
		else if ( !video.getLikesUsers().contains( principal.getName() ) ) { response.setStatus(CODE_STATUS_BAD_REQUEST); }
		else {
			video.setLikes( video.getLikes() - 1 ); // Decrement likes
			video.getLikesUsers().remove( principal.getName() ); // Add user name

			videoRepo.save(video); // persist the object
			
			response.setStatus(CODE_STATUS_OK);
		}
	}
	
	/**
	 * GET /video/{id}/likedby
	 *    - Returns a list of the string usernames of the users that have liked the specified
	 *      video. If the video is not found, a 404 error should be generated.
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method=RequestMethod.GET)
	public @ResponseBody Collection<String> getUsersWhoLikedVideo(
			@PathVariable("id") long id,
    		HttpServletResponse response /**
			 * Any Controller method can take an HttpServletRequest or HttpServletResponse as parameters to gain
			 * low-level access/control over the HTTP messages. Spring will automatically fill in these parameters
			 * when your Controller's method is invoked.
			 * 
			 * Maybe you want to set the status code with the response
			 * or write some binary data to an OutputStream obtained from
			 * the HttpServletResponse object
			 */) {
		List<String> ret = null;
		
		Video video = videoRepo.findOne(id);
		if (video == null) { response.setStatus(CODE_STATUS_NOT_FOUND); }
		else {
			ret = video.getLikesUsers();
		}
		
		return ret;
	}
	
	/**
	 * GET /video/search/findByName?title={title}
	 *    - Returns a list of videos whose titles match the given parameter or an empty
	 *      list if none are found.
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(@RequestParam(VideoSvcApi.TITLE_PARAMETER) String title) {
		Collection<Video> ret = videoRepo.findByName(title);
		return ret;
	}
	
	/**
	 * GET /video/search/findByDurationLessThan?duration={duration}
	 *    - Returns a list of videos whose durations are less than the given parameter or
	 *      an empty list if none are found.
	 */
	@RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDurationLessThan(@RequestParam(VideoSvcApi.DURATION_PARAMETER) long duration) {
		Collection<Video> ret = videoRepo.findByDurationLessThan(duration);
		return ret;
	}
}
