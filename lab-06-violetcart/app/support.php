<?php
require_once __DIR__ . '/includes/layout.php';
$tickets = db()->query('SELECT * FROM support_tickets WHERE visibility = "public" ORDER BY id ASC')->fetchAll();
page_header('Support');
?>
<section class="panel">
  <p class="eyebrow">Support desk</p>
  <h1>Migration tickets</h1>
  <p class="muted">Support notes are edited for buyers, but older wording may still reflect internal workflow names.</p>
  <div class="table-wrap">
    <table>
      <thead><tr><th>ID</th><th>Subject</th><th>Status</th><th>Note</th></tr></thead>
      <tbody>
      <?php foreach ($tickets as $ticket): ?>
        <tr>
          <td><?= (int)$ticket['id'] ?></td>
          <td><?= e($ticket['subject']) ?></td>
          <td><span class="badge"><?= e($ticket['status']) ?></span></td>
          <td><?= e($ticket['body']) ?></td>
        </tr>
      <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</section>
<?php page_footer(); ?>
