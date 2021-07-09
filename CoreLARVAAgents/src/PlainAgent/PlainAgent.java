/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PlainAgent;

import TimeHandler.TimeHandler;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import java.util.ArrayList;
import AdminReport.ReportableObject;

/**
 *
 * @author lcv
 */
public abstract class PlainAgent extends Agent {

    protected Behaviour _currentBehaviour = null, _lastBehaviour;
    protected plainBehaviour _plainBehaviour;
    protected ArrayList<myBehaviour> _myBehavioursList;

    public PlainAgent() {
        super();
        _myBehavioursList = new ArrayList<>();
    }

    @Override
    public void setup() {
        _plainBehaviour = new plainBehaviour(this, "REGULAR");
        addBehaviour(_plainBehaviour);
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    protected abstract void plainExecute();

    protected void doExit() {
        doDelete();
    }

    protected boolean canExit() {
        return true;
    }

    protected void doBlock() {
//        System.out.println(getLocalName() + "::" + this._currentBehaviour.getBehaviourName() + " is blocked");
        this._currentBehaviour.block();
    }

    protected void doBlock(int milis) {
//        System.out.println(getLocalName() + "::" + this._currentBehaviour.getBehaviourName() + " is blocked for "+milis+" ms");
        this._currentBehaviour.block(milis);
    }

    protected void doSleep(int milis) {
//        System.out.println(getLocalName() + "::" + this._currentBehaviour.getBehaviourName() + " is blocked for "+milis+" ms");
        this._currentBehaviour.block(milis);
    }

    protected void doSkip() {
        ((myBehaviour) this._currentBehaviour).suspendBehaviour();

    }

    protected ArrayList<myBehaviour> getAllBehaviours() {
        return _myBehavioursList;
    }


    /// Custom behaviours
    public abstract class myBehaviour extends Behaviour {

        protected String _lastInit = "", _lastFinalize = "", _status="";
        protected long _count;
        protected boolean _suspended;

        protected myBehaviour(Agent a, String name) {
            super();
            initBehaviour(a, name, false);
        }

        protected myBehaviour(Agent a, String name, boolean allow) {
            super();
            initBehaviour(a, name, allow);
        }

        protected final void initBehaviour(Agent a, String name, boolean allow) {
            this.setAgent(a);
            this.setBehaviourName(name);
            _myBehavioursList.add(this);
            _count = 0;
            if (allow) {
                allowBehaviour();
            } else {
                suspendBehaviour();
            }
        }

//        public JsonObject reportStatus() {
//            JsonObject res= new JsonObject().
//                    add("report","behaviour").
//                    add("name", getBehaviourName()).
//                    add("last", getBehaviourLastRun()).
//                    add("count", getBehaviourCount());
//            
//            return res;
//        }
//        
        @Override
        public void action() {
            myBehaviourStart();
            if (!_suspended) {
                myBehaviourBody();
            } else {
                doBlock();
            }
            myBehaviourEnd();
        }

        public void forceaction() {
            allowBehaviour();
            action();
        }

        @Override
        public boolean done() {
            return canExit();
        }

        public final void suspendBehaviour() {
            _suspended = true;
        }

        public final void allowBehaviour() {
            _suspended = false;
        }

        public void myBehaviourStart() {
//            System.out.println(getLocalName() + "::" + this.getBehaviourName() + " is fired");
            _currentBehaviour = this;
            _lastBehaviour = this;
            _lastInit="";
//            _lastInit = new TimeHandler().toString();
        }

        public abstract void myBehaviourBody();

        public int myBehaviourEnd() {
//            System.out.println(getLocalName() + "::" + this.getBehaviourName() + " is closing");
            _currentBehaviour = null;
            _lastFinalize = new TimeHandler().toString();
            _count++;
            return 0;
        }

        public void setBehaviourStatus(String info) {
            _status=info;
        }

        public String getBehaviourStatus() {
            return _status;
        }

        public String getBehaviourLastRun() {
            return _lastFinalize;
        }

        public long getBehaviourCount() {
            return _count;
        }
    }

    public class plainBehaviour extends myBehaviour {

        public plainBehaviour(Agent a, String name) {
            super(a, name, true);
        }

        @Override
        public void myBehaviourBody() {
            plainExecute();
            if (canExit()) {
                doExit();
            }
        }
    }
}
