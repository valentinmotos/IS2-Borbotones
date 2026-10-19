import axios from 'axios';
import { getAllProducts } from './api/fetchProducts';
jest.mock('axios', () => jest.fn());
test('omits undefined category and sends selected filters', async () => {
  axios.mockResolvedValue({ data: [] });
  await getAllProducts();
  expect(axios.mock.calls[0][0]).not.toContain('undefined');
  await getAllProducts('category-id', 'type-id');
  expect(axios.mock.calls[1][0]).toContain('categoryId=category-id&typeId=type-id');
});
