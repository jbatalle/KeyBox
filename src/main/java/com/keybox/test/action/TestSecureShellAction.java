/**
 * Copyright 2013 Sean Kavanagh - sean.p.kavanagh6@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.keybox.test.action;

import com.keybox.common.util.AuthUtil;
import com.keybox.manage.model.*;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This action will create composite ssh terminals to be used
 * Josep Batallé (josep.batalle@i2cat.net)
 */
public class TestSecureShellAction extends ActionSupport implements ServletRequestAware, ServletResponseAware {

    List<SessionOutput> outputList;
    String command;
    HttpServletResponse servletResponse;
    HttpServletRequest servletRequest;
    List<Long> systemSelectId;
    HostSystem currentSystemStatus;
    HostSystem pendingSystemStatus;
    String password;
    String passphrase;
    Long id;
    List<HostSystem> systemList = new ArrayList<HostSystem>();

    static Map<Long, UserSchSessions> userSchSessionMap = new ConcurrentHashMap<Long, UserSchSessions>();

    /**
     * creates composite terminals if there are errors or authentication issues.
     */
    @Action(value = "/test/createTerms",
            results = {
                    @Result(name = "success", location = "/test/secure_shell.jsp")
            }
    )
    public String createTerms() {
        System.out.println("Create Terms function");
        Long userId = AuthUtil.getUserId(servletRequest.getSession());
        Long sessionId = AuthUtil.getSessionId(servletRequest.getSession());
        System.out.println(sessionId);
        System.out.println(pendingSystemStatus);
        if (pendingSystemStatus != null && pendingSystemStatus.getId() != null) {
            System.out.println("Create Terms inside if");
        }
        setSystemList(userId, sessionId);

        return SUCCESS;
    }


    @Action(value = "/test/getNextPendingSystemForTerms",
            results = {
                    @Result(name = "success", location = "/test/secure_shell.jsp")
            }
    )
    public String getNextPendingSystemForTerms() {
System.out.println("getNextPending");        
        Long userId = AuthUtil.getUserId(servletRequest.getSession());
        
        //set system list if no pending systems
        if (pendingSystemStatus == null) {
            setSystemList(userId, AuthUtil.getSessionId(servletRequest.getSession()));
        }

        return SUCCESS;
    }

    @Action(value = "/test/selectSystemsForCompositeTerms",
            results = {
                    @Result(name = "success", location = "/test/secure_shell.jsp")
            }
    )
    public String selectSystemsForCompositeTerms() {


        Long userId = AuthUtil.getUserId(servletRequest.getSession());
        //exit any previous terms
        exitTerms();
        if (systemSelectId != null && !systemSelectId.isEmpty()) {
            //check to see if user has perms to access selected systems

         
//            AuthUtil.setSessionId(servletRequest.getSession(), SessionAuditDB.createSessionLog(userId));


        }
        return SUCCESS;
    }


    @Action(value = "/test/exitTerms",
            results = {
                    @Result(name = "success", location = "/test/menu.action", type = "redirect")

            }
    )
    public String exitTerms() {


        return SUCCESS;
    }

    @Action(value = "/test/disconnectTerm")
    public String disconnectTerm() {
        Long sessionId = AuthUtil.getSessionId(servletRequest.getSession());
        if (TestSecureShellAction.getUserSchSessionMap() != null) {
            UserSchSessions userSchSessions = TestSecureShellAction.getUserSchSessionMap().get(sessionId);
            if (userSchSessions != null) {
                SchSession schSession = userSchSessions.getSchSessionMap().get(id);

                //disconnect ssh session
                schSession.getChannel().disconnect();
                schSession.getSession().disconnect();
                schSession.setChannel(null);
                schSession.setSession(null);
                schSession.setInputToChannel(null);
                schSession.setCommander(null);
                schSession.setOutFromChannel(null);
                schSession = null;
                //remove from map
                userSchSessions.getSchSessionMap().remove(id);
            }


        }


        return null;
    }

    /**
     * set system list once all connections have been attempted
     *
     * @param userId    user id
     * @param sessionId session id
     */
    private void setSystemList(Long userId, Long sessionId) {


        //check user map
        if (userSchSessionMap != null && !userSchSessionMap.isEmpty() && userSchSessionMap.get(sessionId)!=null) {

            //get user sessions
            Map<Long, SchSession> schSessionMap = userSchSessionMap.get(sessionId).getSchSessionMap();


            for (SchSession schSession : schSessionMap.values()) {
                //add to host system list
                systemList.add(schSession.getHostSystem());
                //run script it exists
                
            }
        }

    }

    public List<SessionOutput> getOutputList() {
        return outputList;
    }

    public void setOutputList(List<SessionOutput> outputList) {
        this.outputList = outputList;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    public void setServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    public List<Long> getSystemSelectId() {
        return systemSelectId;
    }

    public void setSystemSelectId(List<Long> systemSelectId) {
        this.systemSelectId = systemSelectId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public HostSystem getCurrentSystemStatus() {
        return currentSystemStatus;
    }

    public void setCurrentSystemStatus(HostSystem currentSystemStatus) {
        this.currentSystemStatus = currentSystemStatus;
    }

    public HostSystem getPendingSystemStatus() {
        return pendingSystemStatus;
    }

    public void setPendingSystemStatus(HostSystem pendingSystemStatus) {
        this.pendingSystemStatus = pendingSystemStatus;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    public List<HostSystem> getSystemList() {
        return systemList;
    }

    public void setSystemList(List<HostSystem> systemList) {
        this.systemList = systemList;
    }

    public static Map<Long, UserSchSessions> getUserSchSessionMap() {
        return userSchSessionMap;
    }

    public static void setUserSchSessionMap(Map<Long, UserSchSessions> userSchSessionMap) {
        TestSecureShellAction.userSchSessionMap = userSchSessionMap;
    }

}


