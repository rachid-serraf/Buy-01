import { PricePipe } from './price.pipe';

describe('PricePipe', () => {
  const pipe = new PricePipe();

  it('always shows a dollar sign and two decimals', () => {
    expect(pipe.transform(129)).toBe('$129.00');
    expect(pipe.transform(4.5)).toBe('$4.50');
    expect(pipe.transform(0)).toBe('$0.00');
  });

  it('rounds to the nearest cent', () => {
    expect(pipe.transform(42.999)).toBe('$43.00');
  });
});
