package org.jlab.clara.sys;

import org.jlab.clara.base.CBase;
import org.jlab.clara.base.CException;
import org.jlab.clara.util.*;
import org.jlab.coda.xmsg.core.xMsgConstants;
import org.jlab.coda.xmsg.core.xMsgUtil;
import org.jlab.coda.xmsg.data.xMsgD;
import org.jlab.coda.xmsg.excp.xMsgException;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>
 *     Service creates a memory mapped file,
 *     i.e. named shared memory in the virtual
 *     memory space of the node.
 *
 *     At the constructor stage memory mapped
 *     file will be created wit the name:
 *     <b>jclara_containerName_serviceEngineName_uniqueId</b>
 *
 *     Unique ID will be generated by calling singleton
 *     object getUniqueId method. THis singleton class can
 *     be part of the Clara base class.
 *
 *     If receiving service (based on the c_composition)
 *     is local (i.e. on the same node) xMsg publish
 *     will have a construct:
 *     <b>topic, sender, String = memoryMappedFileName</b>
 *     otherwise if service next in the chain is remote,
 *     xMsg publish will have a construct:
 *     <b>topic, sender, xMsgData_object</b>
 * </p>
 *
 * @author gurjyan
 * @version 1.x
 * @since 1/30/15
 */
public class Service extends CBase {


    // Already recorded (previous) composition
    private String
            p_composition = xMsgConstants.UNDEFINED.getStringValue();

    // user provided engine class container class name
    private String
            engine_class_name = xMsgConstants.UNDEFINED.getStringValue();

    // Engine instantiated object
    private ACEngine
            engine_object = null;

    // The dynamic (updated for every request) repository/map
    // (mapped by the composition) of input-linked service
    // names that are required to be logically AND-ed
    private HashMap<String, List<String>>
            in_and_name_list = new HashMap<>();

    // The dynamic ( updated for every request) repository/map
    // (mapped by the composition string) of input-linked service
    // data that are required to be logically AND-ed
    private HashMap<String, HashMap<String,CTransit>>
            in_and_data_list = new HashMap<>();

    // Local map of input-linked services for every
    // composition in multi-composition application.
    // Note: by design compositions are separated by ";"
    private HashMap<String, List<String>>
            in_links = new HashMap<>();

    // Local map of output-linked services for every
    // composition in multi-composition application.
    private HashMap<String, List<String>>
            out_links = new HashMap<>();

    // key in the shared memory map of DPE to
    // locate this service resulting data object
    private String
            sharedMemoryKey = xMsgConstants.UNDEFINED.getStringValue();

    // Simple average of the service engine
    // execution times ove all received requests
    private long _avEngineExecutionTime;

    // Number of received requests to this service.
    // Note: common for different compositions
    private long _numberOfRequests;

    // FE host IP
    private String feHost = xMsgConstants.UNDEFINED.getStringValue();

    /**
     * <p>
     * Constructor
     * </p>
     *
     * @param packageName service engine package name
     * @param name   Clara service canonical name
     *               (such as dep:container:engine)
     * @param sharedMemoryKey key in the shared memory map of DPE to
     *                        locate this service resulting data object
     * @param feHost front-end host name. This is the host that holds
     *               centralized registration database.
     * @throws xMsgException
     */
    public Service(String packageName,
                   String name,
                   String sharedMemoryKey,
                   String feHost)
            throws xMsgException,
            CException,
            SocketException,
            IllegalAccessException,
            InstantiationException,
            ClassNotFoundException {
        super(feHost);
        setName(name);

        this.feHost = feHost;

        this.sharedMemoryKey = sharedMemoryKey;

        this.engine_class_name = packageName+"."+CUtility.getEngineName(getName());

        // Dynamic loading of the Clara engine class
        // Note: using system class loader
        CClassLoader cl = new CClassLoader(ClassLoader.getSystemClassLoader());
        engine_object = cl.load(engine_class_name);


        // Create a socket connections
        // to the local dpe proxy
        connect();

        // Send service_up message to the FE
        genericSend(CConstants.SERVICE + ":" + feHost, CConstants.SERVICE_UP+"?"+getName());

        System.out.println("\n"+CUtility.getCurrentTimeInH()+": Started service = "+getName());
        register();

    }

    /**
     * <p>
     * Constructor
     * </p>
     *
     * @param packageName service engine package name
     * @param name Clara service canonical name
     *             (such as dep:container:engine)
     * @param sharedMemoryKey key in the shared memory map of DPE to
     *                        locate this service resulting data object
     * @throws xMsgException
     */
    public Service(String packageName,
                   String name,
                   String sharedMemoryKey)
            throws xMsgException,
            CException,
            SocketException,
            IllegalAccessException,
            InstantiationException,
            ClassNotFoundException {
        super();
        setName(name);
        this.sharedMemoryKey = sharedMemoryKey;
        this.engine_class_name = packageName+"."+CUtility.getEngineName(getName());

        // Dynamic loading of the Clara engine class
        // Note: using system class loader
        CClassLoader cl = new CClassLoader(ClassLoader.getSystemClassLoader());
        engine_object = cl.load(engine_class_name);

        // Create a socket connections
        // to the local dpe proxy
        connect();

        System.out.println("\n"+CUtility.getCurrentTimeInH()+": Started service = "+getName());
        register();

    }

    /**
     * Service configure method
     *
     * @param objectPool reference to the object pool that is
     *                   used to put back the object after the execution
     * @param dataType describes the type of the data object (string or xMsgData)
     * @param data     xMsg envelope payload
     * @param syncReceiverName the name of the sync requester
     *
     * @throws CException
     * @throws xMsgException
     * @throws InterruptedException
     */
    public void configure(LinkedBlockingQueue<Service> objectPool,
                          String dataType,
                          Object data,
                          String syncReceiverName)
            throws CException, xMsgException, InterruptedException {

        xMsgD.Data.Builder inData = null;

        Object userData;

        CTransit engineInData = null;

        String sharedMemoryPointer;

        if (dataType.equals(xMsgConstants.ENVELOPE_DATA_TYPE_STRING.getStringValue())) {
            sharedMemoryPointer = (String) data;

            // get inData from the shared memory
            inData = Dpe.sharedMemory.get(sharedMemoryPointer);

            //user data may also be un-serialized
            userData = Dpe.sharedDataObject.get(sharedMemoryPointer);

            if(userData!=null){
                inData.setDataType(xMsgD.Data.DType.T_OBJECT);
            }

            engineInData = new CTransit(inData, userData);

        } else if (dataType.equals(xMsgConstants.ENVELOPE_DATA_TYPE_XMSGDATA.getStringValue())) {
            inData = (xMsgD.Data.Builder) data;
            engineInData = new CTransit(inData, null);
        }

        if(inData==null)throw new CException("unknown data type");
        // check to see if this is a configure request
        if (inData.getAction().equals(xMsgD.Data.ControlAction.CONFIGURE)){
            engine_object.configure(engineInData);

            // If this is a sync request send done to the requester
            if(!syncReceiverName.equals(xMsgConstants.UNDEFINED.getStringValue())){
                genericSend(syncReceiverName, xMsgConstants.DONE.getStringValue());
            }
        }
        // return this object to the pool
        objectPool.put(this);

    }

    /**
     * Service process method. Note that configure
     * will never be execute within this method.
     *
     * @param objectPool reference to the object pool that is
     *                   used to put back the object after the execution
     * @param dataType describes the type of the data object (string or xMsgData)
     * @param data     xMsg envelope payload
     * @param syncReceiverName the name of the sync requester
     * @param id request id
     *
     * @throws CException
     * @throws xMsgException
     * @throws InterruptedException
     */
    public void process(LinkedBlockingQueue<Service> objectPool,
                        String dataType,
                        Object data,
                        String syncReceiverName,
                        int id)
            throws CException, xMsgException, SocketException, InterruptedException {

        xMsgD.Data.Builder inData = null;

        Object userData;

        CTransit engineInData = null;

        String sharedMemoryPointer;

        // Variables to measure service
        // engine execution time
        long startTime;
        long endTime;

        if (dataType.equals(xMsgConstants.ENVELOPE_DATA_TYPE_STRING.getStringValue())) {
            sharedMemoryPointer = (String) data;

            // get inData from the shared memory
            inData = Dpe.sharedMemory.get(sharedMemoryPointer);

            //user data may also be un-serialized
            userData = Dpe.sharedDataObject.get(sharedMemoryPointer);

            engineInData = new CTransit(inData, userData);

        } else if (dataType.equals(xMsgConstants.ENVELOPE_DATA_TYPE_XMSGDATA.getStringValue())) {
            inData = (xMsgD.Data.Builder) data;
            engineInData = new CTransit(inData, null);
        }

        if(inData==null)throw new CException("unknown data type");

        String c_composition = inData.getComposition();
        String senderService = inData.getSender();


        // check to see if this is a configure request
        if (inData.getAction().equals(xMsgD.Data.ControlAction.CONFIGURE)){
            engine_object.configure(engineInData);

            // If this is a sync request send done to the requester
            if(!syncReceiverName.equals(xMsgConstants.UNDEFINED.getStringValue())){
                genericSend(syncReceiverName, xMsgConstants.DONE.getStringValue());
            }

        } else if (inData.getAction().equals(xMsgD.Data.ControlAction.EXECUTE)) {

            if (!c_composition.equals(p_composition)) {
                // analyze composition
                analyzeComposition(c_composition);
                p_composition = c_composition;
            }

            // Execute service engine
            CTransit service_result = null;

            for (String com : in_links.keySet()) {

                // find a sub_composition that sender
                // service is listed as a an input service
                if (com.contains(senderService)) {

                    // Find if the data from this input service
                    // is required to be logically ANDed with
                    // other input service.
                    // Go over all sub_compositions that require
                    // logical AND of inputs
                    if (in_and_name_list.containsKey(com)) {

                        // Get that sub composition and check against
                        // the received service name the list of service
                        // that are required to be logically ANDed
                        for (String ser : in_and_name_list.get(com)) {
                            if (ser.equals(senderService)) {
                                if (in_and_data_list.containsKey(com)) {
                                    HashMap<String, CTransit> dm = in_and_data_list.get(com);
                                    dm.put(senderService, engineInData);
                                } else {
                                    HashMap<String, CTransit> dm = new HashMap<>();
                                    dm.put(senderService, engineInData);
                                    in_and_data_list.put(com, dm);
                                }
                            }
                        }

                        // Now check the size of received data list
                        // with the required input name list.
                        // If equal we will execute the service.
                        if (in_and_name_list.get(com).size() == in_and_data_list.get(com).size()) {

                            List<CTransit> ddl = new ArrayList<>();

                            for (HashMap<String, CTransit> m : in_and_data_list.values()) {
                                for (CTransit d : m.values()) {
                                    ddl.add(d);
                                }
                            }
                            System.out.println(senderService + ": Executing engine (logAND) = " + engine_class_name);
                            try {
                                // increment request count
                                _numberOfRequests++;
                                // get engine execution start time
                                startTime = System.nanoTime();

                                service_result = engine_object.execute_group(ddl);

                                // get engine execution end time
                                endTime = System.nanoTime();
                                // service engine execution time
                                long execTime = endTime - startTime;
                                // Update transient data with this service execution time
                                service_result.getTransitData().setExecutionTime(execTime);
                                // Calculate a simple average for the execution time
                                _avEngineExecutionTime = (_avEngineExecutionTime + execTime)/ _numberOfRequests;

                            } catch (Throwable t){
                                report_error(t.getMessage(),3, engineInData.getTransitData().getId());
                                return;
                            }
                            // Clear inAnd data hash map for the satisfied composition
                            in_and_data_list.remove(com);
                            break;
                        }
                    } else {

                        // sub-composition does not require logical
                        // AND operations at the input of this service
                        System.out.println(senderService + ": Executing engine = " + engine_class_name);
                        try{
                            // increment request count
                            _numberOfRequests++;
                            // get engine execution start time
                            startTime = System.nanoTime();

                            service_result = engine_object.execute(engineInData);

                            // get engine execution end time
                            endTime = System.nanoTime();
                            // service engine execution time
                            long execTime = endTime - startTime;
                            // Update transient data with this service execution time
                            service_result.getTransitData().setExecutionTime(execTime);
                            // Calculate a simple average for the execution time
                            _avEngineExecutionTime = (_avEngineExecutionTime + execTime)/ _numberOfRequests;

                        } catch (Throwable t){
                            report_error(t.getMessage(),3, engineInData.getTransitData().getId());
                            return;
                        }
                        break;
                    }

                } else if (senderService.startsWith("orchestrator")) {
                    System.out.println(" Orchestrator: Executing engine = " + engine_class_name);
                    try{
                        // increment request count
                        _numberOfRequests++;
                        // get engine execution start time
                        startTime = System.nanoTime();

                        service_result = engine_object.execute(engineInData);

                        // get engine execution end time
                        endTime = System.nanoTime();
                        // service engine execution time
                        long execTime = endTime - startTime;
                        // Update transient data with this service execution time
                        service_result.getTransitData().setExecutionTime(execTime);
                        // Calculate a simple average for the execution time
                        _avEngineExecutionTime = (_avEngineExecutionTime + execTime)/ _numberOfRequests;

                    } catch (Throwable t){
                        report_error(t.getMessage(),3, engineInData.getTransitData().getId());
                        return;
                    }
                    break;
                }
            }

            xMsgD.Data.Builder res = xMsgD.Data.newBuilder();
            Object userObj = null;
            if(service_result==null){
                res.setDataGenerationStatus(xMsgD.Data.Severity.WARNING);
                res.setStatusText(getName()+ ": engine null output");
                res.setStatusSeverityId(1);
            } else {
                res = service_result.getTransitData();
                userObj = service_result.getUserObject();
            }
            res.setSender(getName());

            // Negative id means the service just
            // simply passes the recorded id across
            if (id > 0) res.setId(id);

            // Ensure the output data has the composition
            if (res.getComposition().isEmpty()) res.setComposition(c_composition);

            // Send service engine execution data
            serviceSend(res, userObj);

            // If this is a sync request send data also to the requester
            if(!syncReceiverName.equals(xMsgConstants.UNDEFINED.getStringValue())){
                genericSend(syncReceiverName,res);
            }

            // If engine defines status error
            // or warning broadcast exception
            if(res.getDataGenerationStatus().equals(xMsgD.Data.Severity.ERROR)){
                report_error(res.getStatusText(),res.getStatusSeverityId(), res.getId());
            } else if(res.getDataGenerationStatus().equals(xMsgD.Data.Severity.WARNING)){
                report_warning(res.getStatusText(), res.getStatusSeverityId(), res.getId());
            }
        }

        // return this object to the pool
        objectPool.put(this);
    }


    private void analyzeComposition(String composition) throws CException {
        // This is new routing (composition)  request
        // clear local input-link dictionary and output-links list
        in_links.clear();
        out_links.clear();
        in_and_name_list.clear();
        in_and_data_list.clear();

        // parse the new composition to find input and output
        // linked service names, but first check to see if we
        // have multiple parallel compositions (branching)

        if (composition.contains(";")){
            StringTokenizer st = new StringTokenizer(composition,";");
            while(st.hasMoreTokens()){
                String sub_comp = st.nextToken();
                if(sub_comp.contains(getName())){

                    List<String> il = parse_linked(getName(), sub_comp, 0);
                    in_links.put(sub_comp, il);

                    List<String> ol = parse_linked(getName(), sub_comp, 1);
                    out_links.put(sub_comp, ol);

                    if(is_log_and(getName(), sub_comp)){
                        in_and_name_list.put(sub_comp,il);
                    }
                }
            }
        } else {
            if(composition.contains(getName())){
                List<String> il = parse_linked(getName(), composition, 0);
                in_links.put(composition, il);

                List<String> ol = parse_linked(getName(), composition, 1);
                out_links.put(composition, ol);

                if(is_log_and(getName(), composition)){
                    in_and_name_list.put(composition,il);
                }
            }
        }


    }

    private void serviceSend(xMsgD.Data.Builder data, Object userObj)
            throws xMsgException, SocketException, CException {


        // If data monitors are registered broadcast data
        if (data.getDataMonitor()){
            report_data(data, data.getDataGenerationStatus().name(),data.getStatusSeverityId());
        }

        // If done monitors are registered broadcast done,
        // informing that service is completed
        if (data.getDoneMonitor()) {
            report_done(data.getId());
        }

        // Send to all output-linked services.
        // Note: multiple sub compositions
        for (List<String> ls:out_links.values()){
            for(String ss:ls) {
                if (CUtility.isRemoteService(ss)) {
                    serviceSend(ss, data);
                } else {
                    if(userObj!=null){
                        Dpe.sharedDataObject.put(sharedMemoryKey,userObj);
                        data.setDataType(xMsgD.Data.DType.T_OBJECT);
                    }
                    // copy data to the shared memory
                    Dpe.sharedMemory.put(sharedMemoryKey,data);
                    serviceSend(ss, sharedMemoryKey);
                }
            }
        }
    }

    /**
     * <p>
     * Note that Clara topic for services are constructed as:
     * dpe_host:container:engine
     * <p/>
     */
    public void register()
            throws xMsgException {
        System.out.println(CUtility.getCurrentTimeInH() + ": " + getName() + " sending registration request.");
        registerSubscriber(getName(),
                xMsgUtil.getTopicDomain(getName()),
                xMsgUtil.getTopicSubject(getName()),
                xMsgUtil.getTopicType(getName()),
                engine_object.get_description());
    }

    /**
     * <p>
     *  Removes service xMsg registration
     * <p/>
     */
    public void remove_registration()
            throws xMsgException {

        removeSubscriberRegistration(getName(),
                xMsgUtil.getTopicDomain(getName()),
                xMsgUtil.getTopicSubject(getName()),
                xMsgUtil.getTopicType(getName()));
    }

    /**
     * <p>
     *    Broadcasts a xMsgData transient data
     *    containing an information-string to a
     *    topic = info:this_service_canonical_name.
     *    Note: that this will also send
     *    service engine execution average time.
     *
     * </p>
     * @param info_string content of the information
     */
    public void report_info(String info_string)
            throws xMsgException {

        // build the xMsgData object
        xMsgD.Data.Builder db = xMsgD.Data.newBuilder();

        db.setSender(getName());
        db.setDataGenerationStatus(xMsgD.Data.Severity.INFO);
        db.setDataType(xMsgD.Data.DType.T_STRING);
        db.setSTRING(info_string);
        db.setExecutionTime(_avEngineExecutionTime);

        genericSend(xMsgConstants.INFO.getStringValue() + ":" +
                getName(), db);
    }

    /**
     * <p>
     *    Broadcasts the average engine execution time to
     *    done:<service_name></service_name>, averageExecutionTime
     *
     * </p>
     */
    public void report_done(int id)
            throws xMsgException {

        genericSend(xMsgConstants.DONE.getStringValue() + ":" +
                getName(), id + "?" + _avEngineExecutionTime);
    }

    /**
     * <p>
     *     Broadcasts a xMsgData transient data
     *     containing data generated by the engine,
     *     i.e. unaltered user engine output data.
     *     Severity = 1 is used to report data.
     *    Note: that the data contains service engine
     *    execution current/instantaneous time
     * </p>
     * @param data xMsgData object
     * @param report_type defines the topic to which data
     *                    will be broadcast. Only xMsgConstants
     *                    INFO/WARNING/ERROR are supported.
     */
    public void report_data(Object data,
                            String report_type,
                            int severity)
            throws xMsgException {

        genericSend(xMsgConstants.DATA.getStringValue() + ":" +
                        getName() + ":" +
                        report_type+ ":" +
                        severity,
                data);
    }

    /**
     * <p>
     *    Broadcasts a xMsgData transient data
     *    containing an warning-string to a
     *    topic = warning:severity:this_service_canonical_name
     *    Note: that this will also send
     *    service engine execution average time.
     *
     * </p>
     * @param warning_string content of the warning message
     * @param severity severity of the warning message
     *                 (accepted id = 1, 2 or 3)
     */
    public void report_warning(String warning_string,
                               int severity, int id)
            throws xMsgException, CException {

        // build the xMsgData object
        xMsgD.Data.Builder db = xMsgD.Data.newBuilder();

        db.setSender(getName());
        db.setDataGenerationStatus(xMsgD.Data.Severity.WARNING);
        db.setDataType(xMsgD.Data.DType.T_STRING);
        db.setSTRING(warning_string);
        db.setId(id);
        db.setExecutionTime(_avEngineExecutionTime);

        genericSend(xMsgConstants.WARNING.getStringValue() + ":" +
                        getName() + ":" + severity,
                db);
    }

    /**
     * <p>
     *    Broadcasts a xMsgData transient data
     *    containing an error-string to a
     *    topic = error:severity:this_service_canonical_name
     *    Note: that this will also send
     *    service engine execution average time.
     *
     * </p>
     * @param error_string content of the error message
     * @param severity severity of the error message
     *                 (accepted id = 1, 2 or 3)
     */
    public void report_error(String error_string,
                             int severity, int id)
            throws xMsgException, CException {

        // build the xMsgData object
        xMsgD.Data.Builder db = xMsgD.Data.newBuilder();

        db.setSender(getName());
        db.setDataGenerationStatus(xMsgD.Data.Severity.ERROR);
        db.setDataType(xMsgD.Data.DType.T_STRING);
        db.setSTRING(error_string);
        db.setId(id);
        db.setExecutionTime(_avEngineExecutionTime);

        genericSend(xMsgConstants.ERROR.getStringValue() + ":" +
                        getName() + ":" + severity,
                db);
    }

    /**
     *
     * @throws xMsgException
     */
    public void dispose() throws xMsgException {
        remove_registration();

        if(!feHost.equals(xMsgConstants.UNDEFINED.getStringValue())) {
            // Send service_up message to the FE
            genericSend(CConstants.SERVICE + ":" + feHost, CConstants.SERVICE_DOWN + "?" + getName());
        }
    }

}
