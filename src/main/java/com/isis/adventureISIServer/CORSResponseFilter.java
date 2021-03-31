/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.isis.adventureISIServer;

import java.io.IOException;
import static javax.ws.rs.HttpMethod.OPTIONS;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author dell
 */
@Provider
public class CORSResponseFilter implements ContainerResponseFilter {
 @Override
 public void filter(ContainerRequestContext requestContext,
ContainerResponseContext responseContext) throws IOException {
 MultivaluedMap<String, Object> headers =
responseContext.getHeaders();
 headers.add("Access-Control-Allow-Origin", "*");
 headers.add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
 headers.add("Access-Control-Allow-Headers", "X-Requested-With,content-Type, X-Codingpedia, authorization,X-User");
 }
}

