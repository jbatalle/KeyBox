<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
<title>KeyBox - Composite Terms</title>
<jsp:include page="../_res/inc/header.jsp"/>

<script type="text/javascript">
$(document).ready(function () {

    $("#set_password_dialog").dialog({
        autoOpen: false,
        height: 225,
        minWidth: 550,
        modal: true
    });
    $("#set_passphrase_dialog").dialog({
        autoOpen: false,
        height: 225,
        minWidth: 550,
        modal: true
    });
    $("#error_dialog").dialog({
        autoOpen: false,
        height: 225,
        minWidth: 550,
        modal: true
    });

    $(".termwrapper").sortable({
        connectWith: ".run_cmd",
        handle: ".term-header",
        zIndex: 10000,
        helper: 'clone'
    });


    $.ajaxSetup({ cache: false });
    $('.droppable').droppable({
        zIndex: 10000,
        tolerance: "touch",
        over: function (event, ui) {
            $('.ui-sortable-helper').addClass('dragdropHover');

        },
        out: function (event, ui) {
            $('.ui-sortable-helper').removeClass('dragdropHover');
        },

        drop: function (event, ui) {
            var id = ui.draggable.attr("id").replace("run_cmd_", "");
            $.ajax({ url: '../shell/disconnectTerm.action?id=' + id, cache: false});
            ui.draggable.remove();

        }
    });

    //submit add or edit form
    $(".submit_btn").button().click(function () {
        <s:if test="pendingSystemStatus!=null">
        $(this).parents('form:first').submit();
        </s:if>
        $("#error_dialog").dialog("close");
    });
    //close all forms
    $(".cancel_btn").button().click(function () {
        $("#set_password_dialog").dialog("close");
        window.location = 'getNextPendingSystemForTerms.action?pendingSystemStatus.id=<s:property value="pendingSystemStatus.id"/>&script.id=<s:if test="script!=null"><s:property value="script.id"/></s:if>';

    });

    //if terminal window toggle active for commands
    $(".run_cmd").click(function () {
        //check for cmd-click / ctr-click
        if (!keys[17] && !keys[91] && !keys[93] && !keys[224]) {
            $(".run_cmd").removeClass('run_cmd_active');
        }

        if ($(this).hasClass('run_cmd_active')) {
            $(this).removeClass('run_cmd_active');
        } else {
            $(this).addClass('run_cmd_active')
        }

    });

    $('#select_all').click(function () {
        $(".run_cmd").addClass('run_cmd_active');
    });

    <s:if test="currentSystemStatus!=null && currentSystemStatus.statusCd=='GENERICFAIL'">
    $("#error_dialog").dialog("open");
    </s:if>
    <s:elseif test="pendingSystemStatus!=null">
    <s:if test="pendingSystemStatus.statusCd=='AUTHFAIL'">
    $("#set_password_dialog").dialog("open");
    </s:if>
    <s:elseif test="pendingSystemStatus.statusCd=='KEYAUTHFAIL'">
    $("#set_passphrase_dialog").dialog("open");
    </s:elseif>
    <s:else>
    <s:if test="currentSystemStatus==null ||currentSystemStatus.statusCd!='GENERICFAIL'">
    $("#composite_terms_frm").submit();
    </s:if>
    </s:else>
    </s:elseif>

    <s:if test="pendingSystemStatus==null">

    $('#dummy').focus();
    var keys = {};

    var termFocus = true;
    $("#match").focus(function () {
        termFocus = false;
    });
    $("#match").blur(function () {
        termFocus = true;
    });

    $(".output").mouseover().mousedown(function () {
        termFocus = false;
    });

    $(document).keypress(function (e) {
        if (termFocus) {
            var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
            var idList = [];
            $(".run_cmd_active").each(function (index) {
                var id = $(this).attr("id").replace("run_cmd_", "");
                idList.push(id);
            });
            if (String.fromCharCode(keyCode) && String.fromCharCode(keyCode) != ''
                    && !keys[91] && !keys[93] && !keys[224] && !keys[27]
                    && !keys[37] && !keys[38] && !keys[39] && !keys[40]
                    && !keys[13] && !keys[8] && !keys[9] && !keys[17]  && !keys[46]) {
                var cmdStr = String.fromCharCode(keyCode);
                connection.send(JSON.stringify({id: idList, command: cmdStr}));
            }

        }
    });
    //function for command keys (ie ESC, CTRL, etc..)
    $(document).keydown(function (e) {
        if (termFocus) {
            var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
            keys[keyCode] = true;
            //prevent default for unix ctrl commands
            if(keys[17] && (keys[83]||keys[81]||keys[67]||keys[220]||keys[90]||keys[72]||keys[87]||keys[85]||keys[82]||keys[68])){
                e.preventDefault();
            }

            //27 - ESC
            //37 - LEFT
            //38 - UP
            //39 - RIGHT
            //40 - DOWN
            //13 - ENTER
            //8 - BS
            //9 - TAB
            //17 - CTRL
            //46 - DEL
            if (keys[27] || keys[37] || keys[38] || keys[39] || keys[40] || keys[13] || keys[8] || keys[9] || keys[17] || keys[46]) {
                var idList = [];
                $(".run_cmd_active").each(function (index) {
                    var id = $(this).attr("id").replace("run_cmd_", "");
                    idList.push(id);
                });
                connection.send(JSON.stringify({id: idList, keyCode: keyCode}));
            }
        }
    });

    $(document).keyup(function (e) {
        var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
        delete keys[keyCode];
        if (termFocus) {
            $('#dummy').focus();
        }
    });

    $(document).click(function (e) {
        if (termFocus) {
            $('#dummy').focus();
        }
        //always change focus unless in match sort
        if (e.target.id!='match') {
            termFocus = true;
        }
    });

    //get cmd text from paste
    $("#dummy").bind('paste', function (e) {
        $('#dummy').focus();
        $('#dummy').val('');
        setTimeout(function () {
            var idList = [];
            $(".run_cmd_active").each(function (index) {
                var id = $(this).attr("id").replace("run_cmd_", "");
                idList.push(id);
            });
            var cmdStr = $('#dummy').val();
            connection.send(JSON.stringify({id: idList, command: cmdStr}));
        }, 100);
    });

    var termMap = {};
    $(".output").each(function (index) {
        var id = $(this).attr("id").replace("output_", "");
        termMap[id] = new Terminal(80, 24);
        termMap[id].open($(this));
    });

    var loc = window.location, ws_uri;
    if (loc.protocol === "https:") {
        ws_uri = "wss:";
    } else {
        ws_uri = "ws:";
    }
    ws_uri += "//" + loc.host + loc.pathname + '/../terms.ws?t=' + new Date().getTime();
//  ws_uri = "wss://" + loc.host + loc.pathname + '/../terms.ws?t=' + new Date().getTime();
//ws_uri = "wss://" + loc.host + ':8443' + loc.pathname + '/../terms.ws?t=' + new Date().getTime();
    
    var connection = new WebSocket(ws_uri);

    // Log errors
    connection.onerror = function (error) {
        console.log('WebSocket Error ' + error);
    };

    // Log messages from the server
    connection.onmessage = function (e) {
        var json = jQuery.parseJSON(e.data);
        $.each(json, function (key, val) {
            if (val.output != '') {
                termMap[val.hostSystemId].write(val.output);
            }
        });

    };

    $('#match_btn').unbind().click(function () {
        $('#match_frm').submit();
    });

    $('#match_frm').submit(function () {
        runRegExMatch();
        return false;
    });

    var matchFunction = null;

    function runRegExMatch() {

        if ($('#match_btn').hasClass('btn-success')) {

            $('#match_btn').switchClass('btn-success', 'btn-danger', 0);
            $('#match_btn').text("Stop");

            matchFunction = setInterval(function () {

                var termMap = [];
                var existingTerms = [];
                $(".run_cmd").each(function () {
                    var matchRegEx = null;
                    try {
                        matchRegEx = new RegExp($('#match').val(), 'g');
                    } catch (ex) {
                    }
                    if (matchRegEx != null) {
                        var attrId = $(this).attr("id");
                        if (attrId && attrId != '') {
                            var id = attrId.replace("run_cmd_", "");

                            var match = $('#output_' + id + ' > .terminal').text().match(matchRegEx);

                            if (match != null) {
                                termMap.push({id: id, no_matches: match.length});
                            }
                            existingTerms.push({id: id});
                        }
                    }
                });

                var sorted = termMap.slice(0).sort(function (a, b) {
                    return a.no_matches - b.no_matches;
                });

                for (var i = 0; i < sorted.length; ++i) {
                    var termId = sorted[i].id;
                    $('#run_cmd_' + termId).prependTo('.termwrapper');
                    if (sorted[sorted.length - i - 1].id != existingTerms[i].id) {
                        $('#run_cmd_' + termId).fadeTo(100, .5).fadeTo(100, 1);
                    }
                }
            }, 5000);
        } else {
            $('#match_btn').switchClass('btn-danger', 'btn-success', 0);
            $('#match_btn').text("Start");
            clearInterval(matchFunction)
        }
    }
    </s:if>
});

</script>

<style>
    .dragdropHover {
        background-color: red;
    }

    .align-right {
        padding: 10px 2px 10px 10px;
        float: right;
    }

    .term-container {
        width: 100%;
        padding: 25px 0px;
        margin: 0px;
    }
</style>


</head>
<body>
<!--
    <div class="term-container container">
        <div class="termwrapper">
                <div id="run_cmd_1" class="run_cmd_active run_cmd">
                    <h6 class="term-header">Test</h6>
                    <div class="term">
                        <div id="output_1" class="output"></div>
                    </div>
                </div>
            <div id="upload_push_dialog" title="Upload &amp; Push">
                <iframe id="upload_push_frame" width="700px" height="300px" style="border: none;">
                </iframe>
            </div>
        </div>
    </div>
-->
<div style="float:right;width:1px;">
                    <textarea name="dummy" id="dummy" size="1"
                            style="border:none;color:#FFFFFF;width:1px;height:1px"></textarea>
                    <input type="text" name="dummy2" id="dummy2" size="1"
                            style="border:none;color:#FFFFFF;width:1px;height:1px"/>
</div>
<!--<div class="navbar navbar-default navbar-fixed-top" role="navigation">
    <div class="container">
        <div class="collapse navbar-collapse">
            <s:if test="pendingSystemStatus==null">
                
                <div class="clear"></div>
            </s:if>
        </div>
    </div>
</div>
-->
<div class="term-container container">
    <div class="termwrapper">
    <s:if test="%{!systemList.isEmpty()}">
        <s:iterator value="systemList">
            <div id="run_cmd_<s:property value="id"/>" class="run_cmd_active run_cmd">
                <h6 class="term-header"><s:property value="displayLabel"/></h6>
                <div class="term">
                    <div id="output_<s:property value="id"/>" class="output"></div>
                </div>
            </div>
        </s:iterator>
     </s:if>
    <s:else>System list is empty. Show form to try the connection</s:else>   
    </div>
</div>
    
<div id="set_password_dialog" title="Enter Password">
    <p class="error"><s:property value="pendingSystemStatus.errorMsg"/></p>

    <p>Enter password for <s:property value="pendingSystemStatus.displayLabel"/>

    </p>
    <s:form id="password_frm" action="createTerms">
        <s:hidden name="pendingSystemStatus.id"/>
        <s:password name="password" label="Password" size="15" value="" autocomplete="off"/>
        <s:if test="script!=null">
            <s:hidden name="script.id"/>
        </s:if>
        <tr>
            <td>&nbsp;</td>
            <td align="left">
                <div class="btn btn-default submit_btn">Submit</div>
                <div class="btn btn-default cancel_btn">Cancel</div>
            </td>
        </tr>
    </s:form>
</div>

<div id="set_passphrase_dialog" title="Enter Passphrase">
    <p class="error"><s:property value="pendingSystemStatus.errorMsg"/></p>

    <p>Enter passphrase for <s:property value="pendingSystemStatus.displayLabel"/></p>
    <s:form id="passphrase_frm" action="createTerms">
        <s:hidden name="pendingSystemStatus.id"/>
        <s:password name="passphrase" label="Passphrase" size="15" value="" autocomplete="off"/>
        <s:if test="script!=null">
            <s:hidden name="script.id"/>
        </s:if>
        <tr>
            <td>&nbsp;</td>
            <td align="left">
                <div class="btn btn-default submit_btn">Submit</div>
                <div class="btn btn-default cancel_btn">Cancel</div>
            </td>
        </tr>
    </s:form>
</div>

<div id="error_dialog" title="Error">
    <p class="error">Error: <s:property value="currentSystemStatus.errorMsg"/></p>

    <p>System: <s:property value="currentSystemStatus.displayLabel"/>

    </p>

    <s:form id="error_frm" action="createTerms">
        <s:hidden name="pendingSystemStatus.id"/>
        <s:if test="script!=null">
            <s:hidden name="script.id"/>
        </s:if>
        <tr>
            <td colspan="2">
                <div class="btn btn-default submit_btn">OK</div>
            </td>
        </tr>
    </s:form>
</div>

<s:form id="composite_terms_frm" action="createTerms">
    <s:hidden name="pendingSystemStatus.id"/>
    <s:if test="script!=null">
        <s:hidden name="script.id"/>
    </s:if>
</s:form>

</body>
</html>
