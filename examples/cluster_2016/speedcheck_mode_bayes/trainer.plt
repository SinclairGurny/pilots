trainer speedcheck_mode_bayes; 
/ * vw: wind speed, aw: wind speed angle 
    va: airspeed,   aa: airspeed angle 
    vg: ground speed, ag: ground angle, 
    mode: the error mode 
* / 
data 
    vw, va, vg, aa, 
    ag, aw, mode using file(speedcheck.csv);
model 
    features: vg - sqrt(vaˆ2 + vwˆ2 + 2 * va * vw * cos((PI/180) * (aw-aa))); 
    labels: mode; 
    algorithm: BayesianClassifier using 
        DynamicBayesClassifier 
        (sigma_scale:2, threshold:200); 
    training: both;
end