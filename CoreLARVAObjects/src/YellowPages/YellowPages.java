/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package YellowPages;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Class to support queries of services in a MAS
 *
 * @author lcv
 */
public class YellowPages {

    protected HashMap<String, ArrayList<String>> byName, byService;

    /**
     * Main constructor and initializer of the double hash map that encodes the
     * yellow pages
     */
    public YellowPages() {
        byName = new HashMap();
        byService = new HashMap();
    }

    /**
     * Add the agent named provider as a source of the service
     *
     * @param service The service provided
     * @param provider The agent who provides the service
     */
    public YellowPages addServiceProvider(String service, String provider) {
        if (byName.get(provider) == null) {
            byName.put(provider, new ArrayList());
        }
        if (byService.get(service) == null) {
            byService.put(service, new ArrayList());
        }
        byService.get(service).add(provider);
        byName.get(provider).add(service);
        return this;
    }

    /**
     * Query the set of registered agents
     *
     * @return A Set with the names of the agents registered in the platform who
     * provide whatever kind of service
     */
    public Set<String> queryAgentList() {
        return byName.keySet();
    }

    /**
     * Query the set of services provided in the platform
     *
     * @return A Set of the services provided by any agent registered in the
     * platform
     */
    public Set<String> queryServiceList() {
        return byService.keySet();
    }

    /**
     * Query who agents provide a given service
     *
     * @param service The service requested
     * @return A set of agents who provide that service
     */
    public Set<String> queryProvidersofService(String service) {
        if (byService.get(service) == null) {
            return Set.copyOf(new ArrayList<String>());
        } else {
            return Set.copyOf(byService.get(service));
        }
    }

    /**
     * Query which services provides a given agent, if any
     *
     * @param provider An agent who could provide services
     * @return A set of agents who provide that service
     */
    public Set<String> queryServicesProvided(String provider) {
        if (byName.get(provider) == null) {
            return Set.copyOf(new ArrayList<String>());
        } else {
            return Set.copyOf(byName.get(provider));
        }
    }
    
    /**
     * Initializes the YellowPages with the answer to a QREF sent to Sphinx
     * @param fromserver The answer received from Sphinx
     * @return A copy of the YellowPage instance
     */
    public YellowPages updateYellowPages(ACLMessage fromserver) {
        try {
            JsonObject jso = Json.parse(fromserver.getContent()).asObject();
            JsonArray jsagents=jso.get("registry").asArray(), jsaservices;
            for (int ia=0; ia<jsagents.size(); ia++) {
                JsonObject jsag = jsagents.get(ia).asObject();
                String name = jsag.getString("agent", "nonamed");
                jsaservices = jsag.get("services").asArray();
                for (int is=0; is<jsaservices.size(); is++) {
                    String service = jsaservices.get(is).asString();
                    this.addServiceProvider(service, name);
                }
            }                        
        }catch(Exception ex) {
            System.err.println("EXCEPTION "+ex.toString());
        }
        return this;
    }
    
   public String prettyPrint() {
        String res="";
        res+="┌┐└┘─│┬┴┼┤├"; 
        res = "┌───────────────────────────────────┬───────────────────────────────────┐\n";
        res+=String.format("│%35s│%-35s│\n", "SERVICES", "AGENTS PROVIDERS");
            res += "├───────────────────────────────────┼───────────────────────────────────┤\n";
        
                
        for (String service : this.queryServiceList()) {
            res+=String.format("│%35s│%-35s│\n", service, " ");
            for (String agents : this.queryProvidersofService(service)) {
                res+=String.format("│%35s│%-35s|\n", " ", agents);
            }
            res += "├───────────────────────────────────┼───────────────────────────────────┤\n";
        }
        return res;
        
    }
}
