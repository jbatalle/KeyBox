package com.keybox.test.action;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.keybox.common.util.AppConfig;
import com.keybox.common.util.AuthUtil;
import com.keybox.manage.db.PrivateKeyDB;
import com.keybox.manage.model.ApplicationKey;
import com.keybox.manage.model.HostSystem;
import com.keybox.manage.model.SchSession;
import com.keybox.manage.model.SessionOutput;
import com.keybox.manage.model.UserSchSessions;
import com.keybox.manage.task.SecureShellTask;
import static com.keybox.manage.util.SSHUtil.SESSION_TIMEOUT;
import static com.opensymphony.xwork2.Action.SUCCESS;
import com.opensymphony.xwork2.ActionSupport;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    String password;
    String passphrase;
    Long id;
    List<HostSystem> systemList = new ArrayList<HostSystem>();
//    SortedSet sortedSet = new SortedSet();
    HostSystem hostSystem = new HostSystem();
    static Map<Long, UserSchSessions> userSchSessionMap = new ConcurrentHashMap<Long, UserSchSessions>();

    /**
     * creates composite terminals if there are errors or authentication issues.
     *
     * @return
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
        hS.setStatusCd(HostSystem.SUCCESS_STATUS);
        hS.setId((long) 1);

        Long userId = (long) 1;
        Long sessionId = AuthUtil.getSessionId(servletRequest.getSession());
        if (sessionId == null) {
            sessionId = (long) 1;
        }
        System.out.println("userId: " + userId);
        System.out.println("sessionId: " + sessionId);
        System.out.println("Settin system list");
        try {
            setSystemList2(userId, sessionId, hS);
        } catch (JSchException ex) {
            Logger.getLogger(TestRunScript.class.getName()).log(Level.SEVERE, null, ex);
        }
        return SUCCESS;
    }

    private void setSystemList2(Long userId, Long sessionId, HostSystem hostSystem) throws JSchException {
        JSch jsch = new JSch();
        SchSession schSession = null;
        try {
            System.out.println("GET APP KEY");
            ApplicationKey appKey = PrivateKeyDB.getApplicationKey();
            System.out.println("GET APP KEY - RECEIVED");
            //check to see if passphrase has been provided
            if (passphrase == null || passphrase.trim().equals("")) {
                passphrase = appKey.getPassphrase();
                //check for null inorder to use key without passphrase
                if (passphrase == null) {
                    passphrase = "";
                }
            }

            System.out.println("AppKey: " + appKey.getId().toString());
            //add private key
            
            jsch.addIdentity(appKey.getId().toString(), appKey.getPrivateKey().trim().getBytes(), appKey.getPublicKey().getBytes(), passphrase.getBytes());

            //create session
            Session session = jsch.getSession(hostSystem.getUser(), hostSystem.getDisplayNm(), hostSystem.getPort());
            session.setPassword("demo");

            //set password if it exists
            if (password != null && !password.trim().equals("")) {
                session.setPassword(password);
            }
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(SESSION_TIMEOUT);
            Channel channel = session.openChannel("shell");
            if ("true".equals(AppConfig.getProperty("agentForwarding"))) {
                ((ChannelShell) channel).setAgentForwarding(true);
            }
            ((ChannelShell) channel).setPtyType("vt102");

            InputStream outFromChannel = channel.getInputStream();

            //new session output
            SessionOutput sessionOutput = new SessionOutput();
            sessionOutput.setHostSystemId(hostSystem.getId());
            sessionOutput.setSessionId(sessionId);

            Runnable run = new SecureShellTask(sessionOutput, outFromChannel);
            Thread thread = new Thread(run);
            thread.start();

            OutputStream inputToChannel = channel.getOutputStream();
            PrintStream commander = new PrintStream(inputToChannel, true);

            channel.connect();

            schSession = new SchSession();
            schSession.setUserId(userId);
            schSession.setSession(session);
            schSession.setChannel(channel);
            schSession.setCommander(commander);
            schSession.setInputToChannel(inputToChannel);
            schSession.setOutFromChannel(outFromChannel);
            schSession.setHostSystem(hostSystem);
            UserSchSessions userSchSession = new UserSchSessions();
            Map<Long, SchSession> m = new HashMap<Long, SchSession>();
            m.put((long) 1, schSession);
            userSchSession.setSchSessionMap(m);
            userSchSessionMap.put((long) 1, userSchSession);
        } catch (Exception e) {
            hostSystem.setErrorMsg(e.getMessage());
            System.out.println("MEssage: "+e.getMessage());
/*            System.out.println(e.getMessage().toLowerCase());
            if (e.getMessage().toLowerCase().contains("userauth fail")) {
                hostSystem.setStatusCd(HostSystem.PUBLIC_KEY_FAIL_STATUS);
            } else if (e.getMessage().toLowerCase().contains("auth fail") || e.getMessage().toLowerCase().contains("auth cancel")) {
                hostSystem.setStatusCd(HostSystem.AUTH_FAIL_STATUS);
            } else {
                hostSystem.setStatusCd(HostSystem.GENERIC_FAIL_STATUS);
            }
*/        
        }

        systemList.add(schSession.getHostSystem());
    }

    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    @Override
    public void setServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    @Override
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
