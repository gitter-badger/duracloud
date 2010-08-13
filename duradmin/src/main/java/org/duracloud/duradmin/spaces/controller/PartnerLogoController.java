/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

package org.duracloud.duradmin.spaces.controller;

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.duracloud.client.ContentStoreManager;
import org.duracloud.duradmin.util.SpaceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * 
 * @author Daniel Bernstein
 *
 */
public class PartnerLogoController implements Controller{

    protected final Logger log = 
        LoggerFactory.getLogger(PartnerLogoController.class);

	private ContentStoreManager contentStoreManager;
	
    public ContentStoreManager getContentStoreManager() {
		return contentStoreManager;
	}

	public void setContentStoreManager(ContentStoreManager contentStoreManager) {
		this.contentStoreManager = contentStoreManager;
	}

	@Override
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try{
			SpaceUtil.streamContent( contentStoreManager.getPrimaryContentStore(), response,"x-duracloud-admin", "logo");
		}catch(Exception ex){
			ServletContext sc = request.getSession().getServletContext();
			InputStream is =  sc.getResourceAsStream("/images/partner_logo_placeholder.png");
			response.setContentType("image/png");
			OutputStream os = response.getOutputStream();
			byte[] buf = new byte[1024]; 
			int read = 0; 
			while ((read = is.read(buf)) >= 0) { 
				os.write(buf, 0, read); 
			} 
			is.close(); 
			os.close(); 		
		}
		
		return null;
	}
}