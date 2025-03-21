package org.nicolie.towersforpgm.preparationTime;
import org.bukkit.Location;

public class MatchConfig {
    // Atributos de la clase
    private Location p1;
    private Location p2;
    private int time;
    private int haste;
    private Long timeStart;

    public MatchConfig(Location p1, Location p2, int time, int haste, Long timeStart) {
        // Inicialización de los atributos
        this.p1 = p1;
        this.p2 = p2;
        this.time = time;
        this.haste = haste;
        this.timeStart = timeStart;
    }

    public boolean isInside(Location loc) {
        // Obtener las coordenadas mínimas y máximas de la región
        double x1 = Math.min(p1.getX(), p2.getX()); // Coordenada mínima en X
        double y1 = Math.min(p1.getY(), p2.getY()); // Coordenada mínima en Y
        double z1 = Math.min(p1.getZ(), p2.getZ()); // Coordenada mínima en Z
        double x2 = Math.max(p1.getX(), p2.getX()); // Coordenada máxima en X
        double y2 = Math.max(p1.getY(), p2.getY()); // Coordenada máxima en Y
        double z2 = Math.max(p1.getZ(), p2.getZ()); // Coordenada máxima en Z

        // Verificar si la ubicación está dentro de la región
        return loc.getX() >= x1 && loc.getX() <= x2 &&
                loc.getY() >= y1 && loc.getY() <= y2 &&
                loc.getZ() >= z1 && loc.getZ() <= z2;
    }
    
    public void setP1(Location p1) {
        this.p1 = p1;
    }

    public Location getP1() {
        return p1;
    }

    public void setP2(Location p2) {
        this.p2 = p2;
    }
    
    public Location getP2() {
        return p2;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public void setHaste(int haste) {
        this.haste = haste;
    }

    public int getHaste() {
        return haste;
    }

    public void setTimeStart(Long timeStart) {
        this.timeStart = timeStart;
    }

    public Long getTimeStart() {
        return timeStart;
    }
}
