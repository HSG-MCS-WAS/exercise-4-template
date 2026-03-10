package task1;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.LINK;
import cartago.ObsProperty;
import cartago.OperationException;

public class WeatherStation extends Artifact {
    
    public void init(){
        defineObsProperty("weather", "sunny");
    }

    @LINK
    public void winterIsComing() {
        ObsProperty prop = getObsProperty("weather");

        if ("sunny".equals(prop.stringValue())){
            prop.updateValue("cloudy");
            
            try {
                ArtifactId blindsId = lookupArtifact("blinds");
                execLinkedOp(blindsId,"winterIsComingWithBlinds");
            } catch (OperationException e) {
                e.printStackTrace();
            }
        }
    }
}
