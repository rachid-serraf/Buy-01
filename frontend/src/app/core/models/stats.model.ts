export interface ProductStat {
  productId: string;
  name: string;
  imageUrl?: string | null;
  quantity: number;
  amountSpent: number;
}

export interface BestSellingProductStat {
  productId: string;
  name: string;
  imageUrl?: string | null;
  unitsSold: number;
  revenue: number;
}

export interface CategoryStat {
  category: string;
  quantity: number;
  amountSpent: number;
}

export interface MonthlyRevenueStat {
  month: string;
  revenue: number;
  unitsSold: number;
}

export interface BuyerStats {
  totalSpent: number;
  ordersCount: number;
  mostBoughtProducts: ProductStat[];
  bestProducts: ProductStat[];
  topCategories: CategoryStat[];
}

export interface SellerStats {
  totalRevenue: number;
  ordersCount: number;
  unitsSold: number;
  bestSellingProducts: BestSellingProductStat[];
  revenueByMonth: MonthlyRevenueStat[];
}
