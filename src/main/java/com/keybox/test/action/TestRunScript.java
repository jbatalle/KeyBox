/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.keybox.test.action;

import com.keybox.common.util.AuthUtil;
import com.keybox.manage.db.ScriptDB;
import com.keybox.manage.db.SystemDB;
import com.keybox.manage.db.SystemStatusDB;
import com.keybox.manage.model.HostSystem;
import com.keybox.manage.model.Profile;
import com.keybox.manage.model.SchSession;
import com.keybox.manage.model.Script;
import com.keybox.manage.model.SessionOutput;
import com.keybox.manage.model.SortedSet;
import com.keybox.manage.util.SSHUtil;
import static com.keybox.test.action.TestSecureShellAction.userSchSessionMap;
import static com.opensymphony.xwork2.Action.INPUT;
import static com.opensymphony.xwork2.Action.SUCCESS;
import com.opensymphony.xwork2.ActionSupport;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;

/**
 *
 * @author Josep Batallé <josep.batalle@i2cat.net>
 */
public class TestRunScript extends ActionSupport implements ServletRequestAware, ServletResponseAware {

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
    SortedSet sortedSet = new SortedSet();
    HostSystem hostSystem = new HostSystem();
    Script script = null;
    List<Profile> profileList= new ArrayList<>();

    
    /**
     * creates composite terminals if there are errors or authentication issues.
     */
    @Action(value = "/test/script",
            results = {
                    @Result(name = "success", location = "/test/script_secure_shell.jsp")
            }
    )
    public String createTerms() {
        HostSystem hS = new HostSystem();
        hS.setDisplayNm("Mininet");
        hS.setHost("mininet");
        hS.setUser("demo");
        hS.setPort(22);
        String returned = saveSystem(hS, "", "demo");
        System.out.println("Returned value when save "+returned);
        
        Long userId = AuthUtil.getUserId(servletRequest.getSession());
        Long sessionId = AuthUtil.getSessionId(servletRequest.getSession());
        if (pendingSystemStatus != null && pendingSystemStatus.getId() != null) {
            //get status
            currentSystemStatus = SystemStatusDB.getSystemStatus(pendingSystemStatus.getId(), userId);
            //if initial status run script
            if (currentSystemStatus != null
                    && (HostSystem.INITIAL_STATUS.equals(currentSystemStatus.getStatusCd())
                    || HostSystem.AUTH_FAIL_STATUS.equals(currentSystemStatus.getStatusCd())
                    || HostSystem.PUBLIC_KEY_FAIL_STATUS.equals(currentSystemStatus.getStatusCd()))
                    ) {

                //set current session
                currentSystemStatus = SSHUtil.openSSHTermOnSystem(passphrase, password, userId, sessionId, currentSystemStatus, userSchSessionMap);
            }
            if (currentSystemStatus != null
                    && (HostSystem.AUTH_FAIL_STATUS.equals(currentSystemStatus.getStatusCd())
                    || HostSystem.PUBLIC_KEY_FAIL_STATUS.equals(currentSystemStatus.getStatusCd()))) {

                pendingSystemStatus = currentSystemStatus;
            } else {
                pendingSystemStatus = SystemStatusDB.getNextPendingSystem(userId);
                //if success loop through systems until finished or need password
                while (pendingSystemStatus != null && currentSystemStatus != null && HostSystem.SUCCESS_STATUS.equals(currentSystemStatus.getStatusCd())) {
                    currentSystemStatus = SSHUtil.openSSHTermOnSystem(passphrase, password, userId, sessionId, pendingSystemStatus, userSchSessionMap);
                    pendingSystemStatus = SystemStatusDB.getNextPendingSystem(userId);
                }
            }
        }
        System.out.println(userId);
        System.out.println(SystemStatusDB.getNextPendingSystem(userId));
        //set system list if no pending systems
        if (SystemStatusDB.getNextPendingSystem(userId) == null) {
            setSystemList(userId, sessionId);
        }
        return SUCCESS;
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
                if (script != null && script.getId() != null && script.getId() > 0) {
                    script = ScriptDB.getScript(script.getId(), userId);
                    BufferedReader reader = new BufferedReader(new StringReader(script.getScript()));
                    String line;
                    try {

                        while ((line = reader.readLine()) != null) {
                            schSession.getCommander().println(line);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
            }
        }

    }

    private String saveSystem(HostSystem hostSystem, String passphrase, String password) {
        String retVal=SUCCESS;
        hostSystem = SSHUtil.authAndAddPubKey(hostSystem, passphrase, password, false);
        if (hostSystem.getId() != null) {
            SystemDB.updateSystem(hostSystem);
        } else {
            hostSystem.setId(SystemDB.insertSystem(hostSystem));
        }
        sortedSet = SystemDB.getSystemSet(sortedSet);
        if (!HostSystem.SUCCESS_STATUS.equals(hostSystem.getStatusCd())) {
            retVal=INPUT;
        }
        return retVal;
    }
    
    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    public void setServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
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
    
}
