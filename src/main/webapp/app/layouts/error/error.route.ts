import { Routes } from '@angular/router';

export const errorRoute: Routes = [
  {
    path: 'error',
    loadComponent: () => import('./error.component'),
    title: 'Trang thông tin lỗi!',
  },
  {
    path: 'accessdenied',
    loadComponent: () => import('./error.component'),
    data: {
      errorMessage: 'Bạn không có quyền xem trang này.',
    },
    title: 'Trang thông tin lỗi!',
  },
  {
    path: '404',
    loadComponent: () => import('./error.component'),
    data: {
      errorMessage: 'Trang không tồn tại.',
    },
    title: 'Trang thông tin lỗi!',
  },
  {
    path: '**',
    redirectTo: '/404',
  },
];
