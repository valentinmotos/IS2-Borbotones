import axios from "axios";
import { API_BASE_URL, API_URLS } from "./constant"


export const getAllProducts = async (id,typeId)=>{
    const params = new URLSearchParams();
    if (id) params.set("categoryId", id);
    if (typeId) params.set("typeId", typeId);
    let url = API_BASE_URL + API_URLS.GET_PRODUCTS + `?${params}`;


    try{
        const result = await axios(url,{
            method:"GET"
        });
        return result?.data;
    }
    catch(err){
        console.error(err);
    }
}

export const getProductBySlug = async (slug)=>{
    const url = API_BASE_URL + API_URLS.GET_PRODUCTS + `?slug=${slug}`;
    try{
        const result = await axios(url,{
            method:"GET",
        });
        return result?.data?.[0];
    }
    catch(err){
        console.error(err);
    }
}