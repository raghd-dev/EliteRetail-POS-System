package com.eliteretail.pos.model;

public record ProductStats(double salesAmount, int quantitySold) {
    public double getSalesAmount() { return salesAmount; }
    public int getQuantitySold() { return quantitySold; }
}
