export const INVENTORY_CONFIG = {
  LOW_STOCK_THRESHOLD: 5,
};

export type StockStatus = 'in-stock' | 'low-stock' | 'out-of-stock';

export function getStockStatus(units: number): StockStatus {
  if (units <= 0) return 'out-of-stock';
  if (units <= INVENTORY_CONFIG.LOW_STOCK_THRESHOLD) return 'low-stock';
  return 'in-stock';
}

export function getStockStatusLabel(status: StockStatus): string {
  switch (status) {
    case 'in-stock': return 'In Stock';
    case 'low-stock': return 'Low Stock';
    case 'out-of-stock': return 'Out of Stock';
  }
}

export function parseUnits(units: string | number): number {
  return typeof units === 'number' ? units : parseFloat(units) || 0;
}

export function parsePrice(price: string | number): number {
  return typeof price === 'number' ? price : parseFloat(price) || 0;
}

export function computeLineValue(units: string | number, price: string | number): number {
  return parseUnits(units) * parsePrice(price);
}

export interface InventorySummary {
  totalValue: number;
  skuCount: number;
  lowStockCount: number;
  outOfStockCount: number;
  totalUnits: number;
}

export function computeInventorySummary(
  items: { units: string | number; price: string | number }[]
): InventorySummary {
  let totalValue = 0;
  let totalUnits = 0;
  let lowStockCount = 0;
  let outOfStockCount = 0;

  for (const item of items) {
    const units = parseUnits(item.units);
    const price = parsePrice(item.price);
    totalValue += units * price;
    totalUnits += units;
    const status = getStockStatus(units);
    if (status === 'low-stock') lowStockCount++;
    if (status === 'out-of-stock') outOfStockCount++;
  }

  return {
    totalValue,
    skuCount: items.length,
    lowStockCount,
    outOfStockCount,
    totalUnits,
  };
}
