# Student Attendance System 📊

A full-stack web application designed for engineering faculty to automate classroom attendance management and visualize student data in real-time.

## 🚀 Features
* **Teacher Authentication**: Secure login portal using **MySQL** for credential verification.
* **Class & Batch Management**: Dynamically organize students by Year (FE, SE, TE, BE), Class, and Batch.
* **Attendance Tracking**: Simple interface to mark students as 'Present' or 'Absent' for specific dates.
* **Data Visualization**: Integrated **Chart.js** to generate interactive pie charts showing attendance percentages.
* **Automated Record Keeping**: Maintains a history of attendance with unique constraints to prevent duplicate entries.

## 🛠️ Tech Stack
* **Backend**: Java (Servlets), JDBC
* **Frontend**: HTML5, CSS3, JavaScript, Chart.js
* **Database**: MySQL
* **Tools**: Apache Tomcat, MySQL Workbench

## 📂 Project Structure
* `src/`: Java source files (Servlets and DB logic).
* `web/`: HTML, CSS, and Client-side JS files.
* `sql/`: Database dump and schema scripts.

## ⚙️ Setup
1. Import the `attendance_system.sql` file into your local MySQL server.
2. Configure the database credentials in `config.properties`.
3. Deploy the project on a Java-compatible web server (e.g., Apache Tomcat).

## 👤 Author
* **Engineering Student** at A.P. Shah Institute of Technology (APSIT).
