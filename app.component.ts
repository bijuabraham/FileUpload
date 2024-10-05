import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  form: any = {
    name: '',
    address: '',
    phone: '',
    email: ''
  };
  files: File[] = [];
  errorMessage: string = '';

  constructor(private http: HttpClient) {}

  onFileChange(event: any) {
    this.files = Array.from(event.target.files);
    const allowedTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    const invalidTypeFiles = this.files.filter(file => !allowedTypes.includes(file.type));
    if (invalidTypeFiles.length > 0) {
      this.errorMessage = 'Only PDF and DOCX files are allowed.';
      this.files = [];
      return;
    }
    if (this.files.length > 5) {
      this.errorMessage = 'You can upload up to 5 files only.';
      this.files = [];
    } else {
      const invalidFiles = this.files.filter(file => file.size > 150 * 1024);
      if (invalidFiles.length > 0) {
        this.errorMessage = 'Each file must be less than 150KB.';
        this.files = [];
      } else {
        this.errorMessage = '';
      }
    }
  }

  onSubmit() {
    if (this.files.length > 0) {
      const formData = new FormData();
      formData.append('name', this.form.name);
      formData.append('address', this.form.address);
      formData.append('phone', this.form.phone);
      formData.append('email', this.form.email);
      this.files.forEach((file) => {
        formData.append('files', file, file.name);
      });
      const apiUrl = 'http://your-api-url/upload'; // Externalize API URL for better environment management
      this.http.post(apiUrl, formData).subscribe(
        (response) => {
          console.log('Upload successful', response);
        },
        (error) => {
          console.error('Error uploading', error);
        }
      );
    }
  }
}
