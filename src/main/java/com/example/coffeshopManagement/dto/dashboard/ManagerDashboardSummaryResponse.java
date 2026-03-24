package com.example.coffeshopManagement.dto.dashboard;

public class ManagerDashboardSummaryResponse {
    private long servingOrdersCount;
    private long activeEmployeesCount;
    private String topMenuItemName;
    private long lowStockCount;

    public long getServingOrdersCount() {
        return servingOrdersCount;
    }

    public void setServingOrdersCount(long servingOrdersCount) {
        this.servingOrdersCount = servingOrdersCount;
    }

    public long getActiveEmployeesCount() {
        return activeEmployeesCount;
    }

    public void setActiveEmployeesCount(long activeEmployeesCount) {
        this.activeEmployeesCount = activeEmployeesCount;
    }

    public String getTopMenuItemName() {
        return topMenuItemName;
    }

    public void setTopMenuItemName(String topMenuItemName) {
        this.topMenuItemName = topMenuItemName;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }
}
