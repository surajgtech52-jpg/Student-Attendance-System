# Student Attendance System 📊

A lightweight, Java-based web application designed to help teachers manage student attendance with real-time data visualization. This project was developed as part of my engineering coursework.

## 🚀 Features
* **Teacher Authentication**: Secure login portal for authorized faculty members.
* **Class & Batch Management**: Dynamically add and organize classes (FE, SE, TE, BE) and specific batches.
* **Attendance Tracking**: Mark students as 'Present' or 'Absent' for specific dates.
* **Data Visualization**: Integrated **Chart.js** to display real-time attendance statistics (Present vs. Absent) via interactive pie charts.
* **Dynamic Student Lists**: Add, remove, and automatically sort students by Roll Number.

## 🛠️ Tech Stack
* **Backend**: Java (Servlets), JDBC.
* **Frontend**: HTML5, CSS3, JavaScript.
* **Database**: MySQL.
* **APIs & Libraries**: 
    * Chart.js (for analytics).
    * FontAwesome (for UI icons).

## 📂 Project Structure
* `src/`: Contains the Java source code, including `LoginServlet.java` and `SimpleLoginServer.java`.
* `web/`: Contains the frontend files like `index.html` and `login_teacher.html`.
* `WEB-INF/`: Contains the `web.xml` configuration for servlet mapping.

*/## ⚙️ Setup & Installation
1.  **Database Setup**: 
    * Create a MySQL database and a table named `ab` with columns for `ID`, `PASSWORD`, and `DateTime`.
2.  **Configuration**:
    * Update your `config.properties` file with your local `db.url`, `db.user`, and `db.pass`.
3.  **Deployment**:
    * Compile the Java files and deploy the project on a server like Apache Tomcat or run the `SimpleLoginServer` directly.?/*

## 👤 Author
* **Engineering Student** at A.P. Shah Institute of Technology (APSIT).

---
