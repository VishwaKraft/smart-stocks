import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { UserService } from 'src/app/services/user.service';

@Component({
  selector: 'app-university',
  templateUrl: './university.component.html',
  styleUrls: ['./university.component.css'],
})
export class UniversityComponent implements OnInit {
  isLogin: boolean;
  constructor(
    private userService: UserService,
    private toastr: ToastrService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.userService.isLoggedIn().subscribe((value) => {
      this.isLogin = value;
    });
    this.isLogin = localStorage.getItem('token') != null;
  }

  redirect() {
    if (this.isLogin === false) {
      this.toastr.error('Please Login !', '', {
        closeButton: true,
        positionClass: 'toast-bottom-right',
      });
    } else {
      this.router.navigate(['/user/sandbox/dashboard']);
    }
  }
}
