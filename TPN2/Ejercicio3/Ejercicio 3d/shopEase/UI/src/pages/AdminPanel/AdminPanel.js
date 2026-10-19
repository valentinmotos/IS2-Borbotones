import React from 'react'
import { Admin, fetchUtils, Resource,withLifecycleCallbacks } from 'react-admin'
import simpleRestProvider from "ra-data-simple-rest";
import ProductList from './ProductList';
import EditProduct from './EditProduct';
import CreateProduct from './CreateProduct';
import CategoryList from './Category/CategoryList';
import CategoryEdit from './Category/CategoryEdit';
import { fileUploadAPI } from '../../api/fileUpload';

import { API_BASE_URL } from '../../api/constant';
const CDN_URL = process.env.REACT_APP_CDN_URL || API_BASE_URL + '/uploads';

const httpClient = (url,options={})=>{

  const token = localStorage.getItem('authToken');
  if(!options.headers) options.headers = new Headers();
  options.headers.set('Authorization',`Bearer ${token}`);
  return fetchUtils.fetchJson(url,options);
}

const dataProvider = withLifecycleCallbacks(simpleRestProvider(API_BASE_URL + '/api',httpClient),[
  {
    resource:"products",
    beforeSave: async (params,dataProvider) =>{
      console.log("Params ",params);
      let requestBody = {
        ...params
      }
      let productResList = params?.productResources ?? [];
      if (params?.thumbnail?.rawFile) {
        const fileName = Date.now() + '-' + params.thumbnail.rawFile.name.replaceAll(' ', '-');
        const formData = new FormData();
        formData.append('file', params.thumbnail.rawFile);
        formData.append('fileName', fileName);
        await fileUploadAPI(formData);
        requestBody.thumbnail = CDN_URL + '/' + fileName;
        productResList = productResList.filter(r => !r.isPrimary);
        productResList.push({ name: 'Thumbnail', type: 'image', isPrimary: true, url: requestBody.thumbnail });
      }

    
      const newProductResList = await Promise.all(productResList?.map(async (productResource)=>{
        if (!productResource?.url?.rawFile) return productResource;
        const fileName = productResource?.url?.rawFile?.name?.replaceAll(' ','-');
        const formData = new FormData();
        formData.append("file",productResource?.url?.rawFile);
        formData.append("fileName",fileName);
        const fileUploadRes = await fileUploadAPI(formData);
        return {
          ...productResource,
          url:CDN_URL+"/"+fileName,
        };
      }));
      //console.log("Params ",params,fileName);
      const request = {
        ...requestBody,
        productResources:newProductResList
      }
      console.log("Request Body ",request);
      return request;
    }
  }
]);

export const AdminPanel = () => {
  return (
    <Admin dataProvider={dataProvider} basename='/admin'>
      <Resource name='products' list={ProductList} edit={EditProduct} create={CreateProduct}/>
      <Resource name='category' list={CategoryList} edit={CategoryEdit}/>
    </Admin>
  )
}
