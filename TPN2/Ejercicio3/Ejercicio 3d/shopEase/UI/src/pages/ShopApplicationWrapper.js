import React, { useEffect } from 'react'
import Navigation from '../components/Navigation/Navigation'
import { Outlet } from 'react-router-dom'
import Spinner from '../components/Spinner/Spinner'
import { useSelector, useDispatch } from 'react-redux'
import { fetchCategories } from '../api/fetchCategories';
import { loadCategories } from '../store/features/category';

const ShopApplicationWrapper = () => {

  const dispatch = useDispatch();
  useEffect(() => {
    fetchCategories().then(data => dispatch(loadCategories(data))).catch(console.error);
  }, [dispatch]);
  const isLoading = useSelector((state)=> state?.commonState?.loading);
  return (
    <div>
        <Navigation />
        <Outlet />
        {isLoading && <Spinner />}
    </div>
  )
}

export default ShopApplicationWrapper