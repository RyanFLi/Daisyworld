import Turtle.BlackDaisy;
import Turtle.Daisy;
import Turtle.WhiteDaisy;

import java.util.HashSet;
import java.util.Random;

public class Simulation {
    private int currentTick = 0;
    private boolean stop = false;
    private int width = 29;
    private int height = 29;
    private Patch[][] patches;
    private int numBlacks = 0;
    private int numWhites = 0;
    Scenario scenario = Scenario.HIGH;
    private float globalTemperature = 0.0f;

    public Simulation() {
        this.patches = new Patch[this.height][this.width];
        for(int i=0; i<this.height; i++){
            for(int j=0; j<this.width; j++){
                patches[i][j] = new Patch();
            }
        }
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public void setup(){
        if (scenario != Scenario.MAINTAIN){
            Params.albedoOfSurface = scenario.getAlbedo();
        }

        Random random = new Random();
        numBlacks = Math.round(this.height*this.width*Params.startPctBlacks);
        numWhites = Math.round(this.height*this.width*Params.startPctWhites);

        //index of occupied patches
        HashSet<Integer> set = new HashSet<>();
        //randomly generate black daisies
        for (int i=0; i<this.numBlacks; i++){
            int index = Util.getRandom(0, this.height*this.width, set);
            Daisy daisy = new BlackDaisy();
            daisy.setAlbedo(Params.albedoOfBlacks);
            daisy.setAge(random.nextInt(Params.maxAge));
            patches[index/this.width][index%this.width].setDaisy(daisy);
        }

        //randomly generate white daisies
        for (int i=0; i<this.numWhites; i++){
            int index = Util.getRandom(0, this.height*this.width, set);
            Daisy daisy = new WhiteDaisy();
            daisy.setAlbedo(Params.albedoOfWhites);
            daisy.setAge(random.nextInt(Params.maxAge));
            patches[index/this.width][index%this.width].setDaisy(daisy);
        }

        this.calcTemperature();
        this.calcGlobalTemperature();
    }

    public void go(){
        if (Params.useMaxTick){
            while (!stop && currentTick < Params.maxTick){
                tick();
            }
        }else{
            while (!stop){
                tick();
            }
        }
    }

    public void tick() {
        this.calcTemperature();
        this.diffuse();
        this.checkSurvivability();
        this.calcGlobalTemperature();

        this.currentTick++;
        System.out.println(currentTick);

        if (scenario == Scenario.RAMP){
            if (this.currentTick > 200 && this.currentTick <= 400){
                Params.solarLuminosity = Params.solarLuminosity + 0.005f;
            }else if(this.currentTick > 600 && this.currentTick <= 850){
                Params.solarLuminosity = Params.solarLuminosity - 0.0025f;
            }
        }
    }

    private void calcTemperature(){
        for(int i=0; i<this.height; i++){
            for(int j=0; j<this.width; j++){
                Patch patch = patches[i][j];
                patch.calcTemperature();
            }
        }
    }

    private void calcGlobalTemperature(){
        float sum = 0;
        for(int i=0; i<this.height; i++){
            for(int j=0; j<this.width; j++){
                Patch patch = patches[i][j];
                sum = sum + patch.getTemperature();
            }
        }
        this.globalTemperature = sum / (this.height*this.width);
    }

    private void diffuse(){
        for(int i=0; i<this.height; i++){
            for(int j=0; j<this.width; j++){
                Patch patch = patches[i][j];
                float diffusionTemp = patch.getTemperature() * Params.diffusePct;
                patch.setTemperature(patch.getTemperature() * (1-Params.diffusePct));
                for (int dr = -1; dr <= +1; dr++) {
                    for (int dc = -1; dc <= +1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int r = i + dr;
                        int c = j + dc;
                        if ((r >= 0) && (r < this.height) && (c >= 0) && (c < this.width)) {
                            patches[r][c].setTemperature(patches[r][c].getTemperature() + diffusionTemp/8.0f);
                        }else {
                            patch.setTemperature(patch.getTemperature() + diffusionTemp/8.0f);
                        }
                    }
                }
            }
        }
    }

    private void checkSurvivability(){
        float seedThreshold = 0.0f;
        for(int i=0; i<this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                Patch patch = patches[i][j];
                if (patch.getDaisy() != null){
                    Daisy daisy = patch.getDaisy();
                    daisy.setAge(daisy.getAge() + 1);

                    if (daisy.getAge() < Params.maxAge - daisy.getAge()){
                        patch.setDaisy(null);
                        return;
                    }

                    seedThreshold = 0.1457f * patch.getTemperature()
                            - 0.0032f * patch.getTemperature()*patch.getTemperature()
                            - 0.6443f;

                    if (Math.random() < seedThreshold){
                        boolean seed = false;
                        HashSet<Integer> set = new HashSet<>();
                        while(!seed && set.size() < 9){
                            int index = Util.getRandom(0,9, set);
                            int dr = index/3 - 1 ;
                            int dc = index%3 - 1 ;
                            int r = i + dr;
                            int c = j + dc;
                            if ((r >= 0) && (r < this.height) && (c >= 0) && (c < this.width)
                                    && patches[r][c].getDaisy() == null) {
                                if (daisy.getName().equals("Black")){
                                    Daisy newDaisy = new BlackDaisy();
                                    newDaisy.setAlbedo(Params.albedoOfBlacks);
                                    patches[r][c].setDaisy(newDaisy);
                                }else {
                                    Daisy newDaisy = new WhiteDaisy();
                                    newDaisy.setAlbedo(Params.albedoOfWhites);
                                    patches[r][c].setDaisy(newDaisy);
                                }
                                seed = true;
                            }
                        }
                    }
                }
            }
        }
    }
}
